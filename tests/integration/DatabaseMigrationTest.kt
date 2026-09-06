package deadlines.integration

import deadlines.config.DatabaseConfig
import deadlines.identity.auth.ExposedSessionRepository
import deadlines.identity.auth.Session
import deadlines.identity.email.EmailToken
import deadlines.identity.email.ExposedEmailTokenRepository
import deadlines.identity.users.ExposedUserRepository
import deadlines.identity.users.ExposedUserCredentialsRepository
import deadlines.identity.users.User
import deadlines.identity.users.UserAlreadyExistsException
import deadlines.identity.users.UserProfile
import deadlines.identity.users.UserStatus
import deadlines.organizations.ActiveMembershipAlreadyExistsException
import deadlines.organizations.ExposedOrganizationRepository
import deadlines.organizations.MembershipRole
import deadlines.organizations.MembershipStatus
import deadlines.organizations.Organization
import deadlines.organizations.OrganizationAlreadyExistsException
import deadlines.organizations.OrganizationContext
import deadlines.organizations.OrganizationMembership
import deadlines.organizations.access.ExposedPermissionRepository
import deadlines.organizations.access.ExposedRoleRepository
import deadlines.organizations.access.Permission
import deadlines.organizations.access.PermissionAlreadyExistsException
import deadlines.organizations.access.Role
import deadlines.organizations.access.RoleInUseException
import deadlines.organizations.invitations.ExposedInvitationRepository
import deadlines.organizations.invitations.InvitationStatus
import deadlines.organizations.invitations.OrganizationInvitation
import deadlines.organizations.members.ExposedMemberRepository
import deadlines.shared.database.DatabaseQuery
import deadlines.shared.database.DatabaseFactory
import kotlinx.coroutines.test.runTest
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.nio.file.Path
import java.sql.DriverManager
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@Testcontainers(disabledWithoutDocker = true)
class DatabaseMigrationTest {
    @Test
    fun `startup applies every migration`() {
        val config =
            DatabaseConfig(
                url = postgres.jdbcUrl,
                user = postgres.username,
                password = postgres.password,
                maximumPoolSize = 2,
                migrationsLocation = migrationLocation(),
            )

        DatabaseFactory.open(config).use {
            DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { connection ->
                connection.prepareStatement(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE success",
                ).use { statement ->
                    statement.executeQuery().use { result ->
                        result.next()
                        assertEquals(10, result.getInt(1))
                    }
                }
            }
        }
    }

    @Test
    fun `user repository persists and reads a complete user`() =
        runTest {
            DatabaseFactory.open(databaseConfig()).use { database ->
                val repository = ExposedUserRepository(DatabaseQuery(database.database))
                val now = Instant.parse("2026-09-05T12:00:00Z")
                val user =
                    User(
                        id = UUID.randomUUID(),
                        email = "repository@example.com",
                        status = UserStatus.ACTIVE,
                        profile = UserProfile("Repo", "Test", null, "+5511999999999"),
                        createdAt = now,
                        updatedAt = now,
                    )

                repository.create(user)
                val found = repository.findByEmail("REPOSITORY@EXAMPLE.COM")

                assertEquals(user, found)
                assertEquals(1, repository.count())
                assertEquals(listOf(user), repository.list(offset = 0, limit = 20))

                val updated = user.copy(status = UserStatus.DISABLED, updatedAt = now.plusSeconds(60))
                repository.update(updated)
                assertEquals(updated, repository.findById(user.id))
            }
        }

    @Test
    fun `database uniqueness violation is exposed as a user conflict`() =
        runTest {
            DatabaseFactory.open(databaseConfig()).use { database ->
                val repository = ExposedUserRepository(DatabaseQuery(database.database))
                val now = Instant.parse("2026-09-05T12:00:00Z")
                val first =
                    User(
                        id = UUID.randomUUID(),
                        email = "unique@example.com",
                        status = UserStatus.PENDING,
                        profile = UserProfile("First", "User", null, null),
                        createdAt = now,
                        updatedAt = now,
                    )
                val duplicate =
                    first.copy(
                        id = UUID.randomUUID(),
                        email = "UNIQUE@EXAMPLE.COM",
                    )

                repository.create(first)

                assertFailsWith<UserAlreadyExistsException> {
                    repository.create(duplicate)
                }
            }
        }

    @Test
    fun `credentials repository persists a password hash without exposing it on the user`() =
        runTest {
            DatabaseFactory.open(databaseConfig()).use { database ->
                val repository = ExposedUserCredentialsRepository(DatabaseQuery(database.database))
                val now = Instant.parse("2026-09-05T12:00:00Z")
                val user =
                    User(
                        id = UUID.randomUUID(),
                        email = "credentials@example.com",
                        status = UserStatus.ACTIVE,
                        profile = UserProfile("Credential", "Test", null, null),
                        createdAt = now,
                        updatedAt = now,
                    )

                repository.create(user, "a-password-hash")
                val credentials = repository.findByEmail("CREDENTIALS@EXAMPLE.COM")

                assertEquals(user, credentials?.user)
                assertEquals("a-password-hash", credentials?.passwordHash)
            }
        }

    @Test
    fun `session repository rotates a refresh token atomically`() =
        runTest {
            DatabaseFactory.open(databaseConfig()).use { database ->
                val query = DatabaseQuery(database.database)
                val users = ExposedUserRepository(query)
                val sessions = ExposedSessionRepository(query)
                val now = Instant.now()
                val user =
                    User(
                        id = UUID.randomUUID(),
                        email = "session-${UUID.randomUUID()}@example.com",
                        status = UserStatus.ACTIVE,
                        profile = UserProfile("Session", "Test", null, null),
                        createdAt = now,
                        updatedAt = now,
                    )
                val first = Session(UUID.randomUUID(), user.id, "a".repeat(64), null, null, now.plusSeconds(60), now)
                val replacement =
                    Session(UUID.randomUUID(), user.id, "b".repeat(64), null, null, now.plusSeconds(120), now)

                users.create(user)
                sessions.create(first)

                assertEquals(first, sessions.findActive(first.refreshTokenHash, now))
                assertEquals(true, sessions.rotate(first.refreshTokenHash, replacement, now))
                assertNull(sessions.findActive(first.refreshTokenHash, now))
                assertEquals(replacement, sessions.findActive(replacement.refreshTokenHash, now))
                assertEquals(false, sessions.rotate(first.refreshTokenHash, replacement, now))
            }
        }

    @Test
    fun `email tokens are single use and replace active tokens`() =
        runTest {
            DatabaseFactory.open(databaseConfig()).use { database ->
                val query = DatabaseQuery(database.database)
                val users = ExposedUserRepository(query)
                val tokens = ExposedEmailTokenRepository(query)
                val now = Instant.now()
                val user =
                    User(
                        id = UUID.randomUUID(),
                        email = "email-token-${UUID.randomUUID()}@example.com",
                        status = UserStatus.ACTIVE,
                        profile = UserProfile("Email", "Token", null, null),
                        createdAt = now,
                        updatedAt = now,
                    )
                users.create(user)
                val first = EmailToken(UUID.randomUUID(), user.id, "c".repeat(64), now.plusSeconds(60), now)
                val replacement = EmailToken(UUID.randomUUID(), user.id, "d".repeat(64), now.plusSeconds(60), now)

                tokens.createVerification(first)
                tokens.createVerification(replacement)

                assertNull(tokens.consumeVerification(first.tokenHash, now))
                assertEquals(user.id, tokens.consumeVerification(replacement.tokenHash, now))
                assertNull(tokens.consumeVerification(replacement.tokenHash, now))
            }
        }

    @Test
    fun `organization repository creates owner membership and updates the current organization`() =
        runTest {
            DatabaseFactory.open(databaseConfig()).use { database ->
                val query = DatabaseQuery(database.database)
                val users = ExposedUserRepository(query)
                val organizations = ExposedOrganizationRepository(query)
                val now = Instant.now()
                val user = testUser("organization-owner", now)
                val context = organizationContext(user.id, "acme-${UUID.randomUUID()}", now)
                users.create(user)

                assertEquals(context, organizations.createWithOwner(context))
                assertEquals(context, organizations.findCurrentByUser(user.id))

                val updated = context.organization.copy(name = "Acme Updated", updatedAt = now.plusSeconds(60))
                organizations.update(updated)
                assertEquals(updated, organizations.findCurrentByUser(user.id)?.organization)
            }
        }

    @Test
    fun `organization repository enforces unique slug and one active membership`() =
        runTest {
            DatabaseFactory.open(databaseConfig()).use { database ->
                val query = DatabaseQuery(database.database)
                val users = ExposedUserRepository(query)
                val organizations = ExposedOrganizationRepository(query)
                val now = Instant.now()
                val firstUser = testUser("organization-first", now)
                val secondUser = testUser("organization-second", now)
                val slug = "unique-${UUID.randomUUID()}"
                users.create(firstUser)
                users.create(secondUser)
                organizations.createWithOwner(organizationContext(firstUser.id, slug, now))

                assertFailsWith<ActiveMembershipAlreadyExistsException> {
                    organizations.createWithOwner(organizationContext(firstUser.id, "another-${UUID.randomUUID()}", now))
                }
                assertFailsWith<OrganizationAlreadyExistsException> {
                    organizations.createWithOwner(organizationContext(secondUser.id, slug.uppercase(), now))
                }
            }
        }

    @Test
    fun `access repositories isolate and persist organization roles and permissions`() =
        runTest {
            DatabaseFactory.open(databaseConfig()).use { database ->
                val query = DatabaseQuery(database.database)
                val users = ExposedUserRepository(query)
                val organizations = ExposedOrganizationRepository(query)
                val permissions = ExposedPermissionRepository(query)
                val roles = ExposedRoleRepository(query)
                val now = Instant.now()
                val user = testUser("access-owner", now)
                val context = organizationContext(user.id, "access-${UUID.randomUUID()}", now)
                users.create(user)
                organizations.createWithOwner(context)

                assertEquals(8, permissions.list(context.organization.id).count { it.isSystem })
                assertEquals(setOf("member", "owner"), roles.list(context.organization.id).map { it.key }.toSet())

                val customPermission =
                    Permission(
                        UUID.randomUUID(),
                        context.organization.id,
                        "deadlines.manage",
                        "Manage deadlines",
                        null,
                        false,
                        now,
                        now,
                    )
                permissions.create(customPermission)
                assertEquals(customPermission, permissions.findById(context.organization.id, customPermission.id))
                val ownerRole = roles.list(context.organization.id).single { it.key == "owner" }
                assertEquals(true, roles.listPermissions(ownerRole.id).contains(customPermission))
                assertFailsWith<PermissionAlreadyExistsException> {
                    permissions.create(customPermission.copy(id = UUID.randomUUID()))
                }

                val customRole =
                    Role(
                        UUID.randomUUID(),
                        context.organization.id,
                        "manager",
                        "Manager",
                        null,
                        false,
                        now,
                        now,
                    )
                roles.create(customRole)
                roles.replacePermissions(customRole.id, listOf(customPermission.id))
                assertEquals(listOf(customPermission), roles.listPermissions(customRole.id))
                assertEquals(true, roles.delete(context.organization.id, customRole.id))
                assertEquals(true, permissions.delete(context.organization.id, customPermission.id))
            }
        }

    @Test
    fun `invitation acceptance atomically creates an organization member`() =
        runTest {
            DatabaseFactory.open(databaseConfig()).use { database ->
                val query = DatabaseQuery(database.database)
                val users = ExposedUserRepository(query)
                val organizations = ExposedOrganizationRepository(query)
                val roles = ExposedRoleRepository(query)
                val invitations = ExposedInvitationRepository(query)
                val members = ExposedMemberRepository(query)
                val now = Instant.now()
                val owner = testUser("invitation-owner", now)
                val invitee = testUser("invitation-member", now)
                val context = organizationContext(owner.id, "invitation-${UUID.randomUUID()}", now)
                users.create(owner)
                users.create(invitee)
                organizations.createWithOwner(context)
                val memberRole = roles.list(context.organization.id).single { it.key == "member" }
                val invitation =
                    OrganizationInvitation(
                        UUID.randomUUID(),
                        context.organization.id,
                        context.organization.name,
                        invitee.email,
                        memberRole,
                        owner.id,
                        "a".repeat(64),
                        InvitationStatus.PENDING,
                        now.plusSeconds(3600),
                        now,
                        now,
                    )

                invitations.create(invitation)
                assertEquals(invitation.id, invitations.findByTokenHash("a".repeat(64), now)?.id)
                assertEquals(true, invitations.accept(invitation, invitee.id, UUID.randomUUID(), now))
                assertEquals(invitee.id, members.findByUserId(context.organization.id, invitee.id)?.userId)
                assertEquals("accepted", invitations.findById(context.organization.id, invitation.id, now)?.status?.name?.lowercase())
                assertFailsWith<RoleInUseException> {
                    roles.delete(context.organization.id, memberRole.id)
                }
            }
        }

    private fun databaseConfig() =
        DatabaseConfig(
            url = postgres.jdbcUrl,
            user = postgres.username,
            password = postgres.password,
            maximumPoolSize = 2,
            migrationsLocation = migrationLocation(),
        )

    private fun testUser(prefix: String, now: Instant) =
        User(
            id = UUID.randomUUID(),
            email = "$prefix-${UUID.randomUUID()}@example.com",
            status = UserStatus.ACTIVE,
            profile = UserProfile("Organization", "Owner", null, null),
            createdAt = now,
            updatedAt = now,
        )

    private fun organizationContext(userId: UUID, slug: String, now: Instant): OrganizationContext {
        val organizationId = UUID.randomUUID()
        return OrganizationContext(
            organization = Organization(organizationId, "Acme", slug, userId, now, now),
            membership =
                OrganizationMembership(
                    UUID.randomUUID(),
                    organizationId,
                    userId,
                    MembershipRole.OWNER,
                    MembershipStatus.ACTIVE,
                    now,
                    null,
                ),
        )
    }

    private fun migrationLocation(): String {
        val migrations = Path.of("../database/migrations").toAbsolutePath().normalize()
        return "filesystem:$migrations"
    }

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }
}
