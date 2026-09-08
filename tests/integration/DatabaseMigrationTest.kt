package deadlines.integration

import deadlines.organizations.audits.*
import deadlines.organizations.OrganizationService
import deadlines.organizations.UpdateOrganizationRequest
import deadlines.organizations.access.*
import deadlines.organizations.authorization.AuthorizationService
import deadlines.organizations.authorization.ExposedAuthorizationRepository
import deadlines.organizations.members.MemberService
import deadlines.organizations.members.UpdateMemberRoleRequest
import kotlin.test.assertTrue
import java.sql.SQLException
import deadlines.config.DatabaseConfig
import deadlines.identity.auth.ExposedSessionRepository
import deadlines.identity.auth.Session
import deadlines.identity.preferences.ExposedUserPreferenceRepository
import deadlines.identity.email.EmailToken
import deadlines.identity.email.ExposedEmailTokenRepository
import deadlines.identity.users.ExposedUserRepository
import deadlines.identity.users.ExposedUserCredentialsRepository
import deadlines.identity.users.ExposedAccountLifecycleRepository
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
import deadlines.organizations.OrganizationStatus
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
import deadlines.subscriptions.ExposedSubscriptionRepository
import kotlinx.coroutines.test.runTest
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.nio.file.Path
import java.sql.DriverManager
import java.time.Instant
import java.time.temporal.ChronoUnit
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
                        assertEquals(20, result.getInt(1))
                    }
                }
            }
        }
    }

    @Test
    fun `creating an organization provisions an active free subscription`() = runTest {
        DatabaseFactory.open(databaseConfig()).use { database ->
            val query = DatabaseQuery(database.database)
            val users = ExposedUserRepository(query)
            val organizations = ExposedOrganizationRepository(query)
            val now = Instant.parse("2026-09-06T20:00:00Z")
            val user = testUser("subscription", now)
            users.create(user)
            val context = organizationContext(user.id, "subscription-${UUID.randomUUID().toString().take(8)}", now)

            organizations.createWithOwner(context)

            val subscription = ExposedSubscriptionRepository(query).findActiveByOrganization(context.organization.id)
            assertEquals("free", subscription?.plan?.key)
            assertEquals(context.organization.id, subscription?.organizationId)
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

                val disabledAt = now.plusSeconds(60)
                val updated = user.copy(status = UserStatus.DISABLED, disabledAt = disabledAt, updatedAt = disabledAt)
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
    fun `new users receive default preferences that can be updated`() =
        runTest {
            DatabaseFactory.open(databaseConfig()).use { database ->
                val query = DatabaseQuery(database.database)
                val users = ExposedUserRepository(query)
                val preferences = ExposedUserPreferenceRepository(query)
                val now = Instant.now()
                val user = User(
                    UUID.randomUUID(),
                    "preferences-${UUID.randomUUID()}@example.com",
                    UserStatus.ACTIVE,
                    UserProfile("Preference", "Test", null, null),
                    now,
                    now,
                )

                users.create(user)
                val defaults = preferences.findByUserId(user.id)
                val updated = preferences.update(user.id, "en", "Europe/Lisbon", "dark", now.plusSeconds(1))

                assertEquals("pt-BR", defaults?.locale)
                assertEquals("America/Sao_Paulo", defaults?.timezone)
                assertEquals("system", defaults?.theme)
                assertEquals("en", updated?.locale)
                assertEquals("Europe/Lisbon", updated?.timezone)
                assertEquals("dark", updated?.theme)
            }
        }

    @Test
    fun `account lifecycle revokes sessions and removes personal data`() = runTest {
        DatabaseFactory.open(databaseConfig()).use { database ->
            val query = DatabaseQuery(database.database)
            val users = ExposedUserRepository(query)
            val credentials = ExposedUserCredentialsRepository(query)
            val sessions = ExposedSessionRepository(query)
            val preferences = ExposedUserPreferenceRepository(query)
            val lifecycle = ExposedAccountLifecycleRepository(query)
            val now = Instant.now()

            val disabled = testUser("disabled-account", now)
            credentials.create(disabled, "disabled-password-hash")
            sessions.create(Session(UUID.randomUUID(), disabled.id, "e".repeat(64), null, null, now.plusSeconds(600), now))
            assertTrue(lifecycle.deactivate(disabled.id, now.plusSeconds(1)))
            assertEquals(UserStatus.DISABLED, users.findById(disabled.id)?.status)
            assertTrue(sessions.listActive(disabled.id, now.plusSeconds(2)).isEmpty())

            val deleted = testUser("deleted-account", now)
            credentials.create(deleted, "deleted-password-hash")
            sessions.create(Session(UUID.randomUUID(), deleted.id, "f".repeat(64), null, null, now.plusSeconds(600), now))
            assertTrue(lifecycle.delete(deleted.id, now.plusSeconds(1)))

            val anonymized = users.findById(deleted.id)
            assertEquals(UserStatus.DELETED, anonymized?.status)
            assertEquals("deleted-${deleted.id}@internal.invalid", anonymized?.email)
            assertEquals("Deleted", anonymized?.profile?.firstName)
            assertEquals("User", anonymized?.profile?.lastName)
            assertNull(credentials.findByUserId(deleted.id))
            assertNull(preferences.findByUserId(deleted.id))
            assertTrue(sessions.listActive(deleted.id, now.plusSeconds(2)).isEmpty())
        }
    }

    @Test
    fun `session repository rotates a refresh token atomically`() =
        runTest {
            DatabaseFactory.open(databaseConfig()).use { database ->
                val query = DatabaseQuery(database.database)
                val users = ExposedUserRepository(query)
                val sessions = ExposedSessionRepository(query)
                val now = Instant.now().truncatedTo(ChronoUnit.MICROS)
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
                    first.copy(
                        refreshTokenHash = "b".repeat(64),
                        expiresAt = now.plusSeconds(120),
                        lastSeenAt = now.plusSeconds(30),
                    )

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
    fun `session repository keeps one session per user and device`() =
        runTest {
            DatabaseFactory.open(databaseConfig()).use { database ->
                val query = DatabaseQuery(database.database)
                val users = ExposedUserRepository(query)
                val sessions = ExposedSessionRepository(query)
                val now = Instant.now().truncatedTo(ChronoUnit.MICROS)
                val user =
                    User(
                        id = UUID.randomUUID(),
                        email = "device-session-${UUID.randomUUID()}@example.com",
                        status = UserStatus.ACTIVE,
                        profile = UserProfile("Device", "Session", null, null),
                        createdAt = now,
                        updatedAt = now,
                    )
                val deviceId = UUID.randomUUID()
                val first = Session(UUID.randomUUID(), user.id, "c".repeat(64), null, null, now.plusSeconds(60), now, deviceId, now)
                val replacement = first.copy(refreshTokenHash = "d".repeat(64), lastSeenAt = now.plusSeconds(30))

                users.create(user)
                sessions.create(first)
                sessions.create(replacement)

                assertEquals(replacement, sessions.findByDevice(user.id, deviceId))
                assertEquals(listOf(replacement), sessions.listActive(user.id, now))
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
                val now = Instant.now().truncatedTo(ChronoUnit.MICROS)
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
    fun `organization lifecycle isolates access and preserves history`() = runTest {
        DatabaseFactory.open(databaseConfig()).use { database ->
            val query = DatabaseQuery(database.database)
            val users = ExposedUserRepository(query)
            val organizations = ExposedOrganizationRepository(query)
            val authorization = ExposedAuthorizationRepository(query)
            val roles = ExposedRoleRepository(query)
            val invitations = ExposedInvitationRepository(query)
            val audits = ExposedAuditRepository(query)
            val now = Instant.now()
            val owner = testUser("organization-lifecycle", now)
            val context = organizationContext(owner.id, "organization-lifecycle-${UUID.randomUUID()}", now)
            users.create(owner)
            organizations.createWithOwner(context)
            val memberRole = roles.list(context.organization.id).single { it.key == "member" }
            val invitation = OrganizationInvitation(
                UUID.randomUUID(), context.organization.id, context.organization.name,
                "pending-${UUID.randomUUID()}@example.com", memberRole, owner.id, "1".repeat(64),
                InvitationStatus.PENDING, now.plusSeconds(3600), now, now,
            )
            invitations.create(invitation)

            assertTrue(organizations.suspend(context.organization.id, now.plusSeconds(1)))
            assertNull(authorization.findByUserId(owner.id))
            assertEquals(OrganizationStatus.SUSPENDED, organizations.findRetainedByUser(owner.id)?.organization?.status)
            assertEquals(OrganizationStatus.ACTIVE, organizations.reactivateOwnedBy(owner.id, now.plusSeconds(2))?.organization?.status)
            assertTrue(authorization.findByUserId(owner.id) != null)

            assertTrue(organizations.delete(context.organization.id, now.plusSeconds(3)))
            assertNull(organizations.findCurrentByUser(owner.id))
            assertNull(organizations.findRetainedByUser(owner.id))
            assertEquals(InvitationStatus.REVOKED, invitations.findById(context.organization.id, invitation.id, now)?.status)
            assertTrue(audits.list(context.organization.id, AuditFilter(limit = 20)).data.any {
                it.action == "organization.deleted"
            })

            val replacement = organizationContext(owner.id, context.organization.slug, now.plusSeconds(4))
            assertEquals(replacement.organization.id, organizations.createWithOwner(replacement).organization.id)
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
                val now = Instant.now().truncatedTo(ChronoUnit.MICROS)
                val user = testUser("access-owner", now)
                val context = organizationContext(user.id, "access-${UUID.randomUUID()}", now)
                users.create(user)
                organizations.createWithOwner(context)

                assertEquals(17, permissions.list(context.organization.id).count { it.isSystem })
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
    fun `authorization repository resolves current tenant role and permissions`() =
        runTest {
            DatabaseFactory.open(databaseConfig()).use { database ->
                val query = DatabaseQuery(database.database)
                val users = ExposedUserRepository(query)
                val organizations = ExposedOrganizationRepository(query)
                val roles = ExposedRoleRepository(query)
                val permissions = ExposedPermissionRepository(query)
                val authorization = ExposedAuthorizationRepository(query)
                val now = Instant.now()
                val user = testUser("authorization-owner", now)
                val context = organizationContext(user.id, "authorization-${UUID.randomUUID()}", now)
                users.create(user)
                organizations.createWithOwner(context)

                val resolved = authorization.findByUserId(user.id)!!
                val ownerRole = roles.list(context.organization.id).single { it.key == "owner" }
                val expectedPermissions = permissions.list(context.organization.id).map { it.key }.toSet()

                assertEquals(context.organization.id, resolved.organizationId)
                assertEquals(context.membership.id, resolved.membershipId)
                assertEquals(ownerRole.id, resolved.roleId)
                assertEquals(expectedPermissions, resolved.permissions)
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

    @Test
    fun `suspended membership reserves the user until removal`() = runTest {
        DatabaseFactory.open(databaseConfig()).use { database ->
            val query = DatabaseQuery(database.database)
            val users = ExposedUserRepository(query)
            val organizations = ExposedOrganizationRepository(query)
            val roles = ExposedRoleRepository(query)
            val invitations = ExposedInvitationRepository(query)
            val members = ExposedMemberRepository(query)
            val now = Instant.now()
            val owner = testUser("lifecycle-owner", now)
            val invitee = testUser("lifecycle-member", now)
            val context = organizationContext(owner.id, "lifecycle-${UUID.randomUUID()}", now)
            users.create(owner)
            users.create(invitee)
            organizations.createWithOwner(context)
            val memberRole = roles.list(context.organization.id).single { it.key == "member" }
            val invitation = OrganizationInvitation(
                UUID.randomUUID(), context.organization.id, context.organization.name, invitee.email, memberRole,
                owner.id, "e".repeat(64), InvitationStatus.PENDING, now.plusSeconds(3600), now, now,
            )
            invitations.create(invitation)
            assertTrue(invitations.accept(invitation, invitee.id, UUID.randomUUID(), now))
            val member = members.findByUserId(context.organization.id, invitee.id)!!

            assertTrue(members.suspend(context.organization.id, member.membershipId))
            assertEquals(MembershipStatus.SUSPENDED, members.findByUserId(context.organization.id, invitee.id)?.status)
            assertFailsWith<ActiveMembershipAlreadyExistsException> {
                organizations.createWithOwner(organizationContext(invitee.id, "blocked-${UUID.randomUUID()}", now))
            }

            assertTrue(members.remove(context.organization.id, member.membershipId, now.plusSeconds(1)))
            assertEquals(invitee.id, organizations.createWithOwner(
                organizationContext(invitee.id, "released-${UUID.randomUUID()}", now),
            ).membership.userId)
        }
    }

    @Test
    fun `ownership transfer preserves exactly one active owner`() = runTest {
        DatabaseFactory.open(databaseConfig()).use { database ->
            val query = DatabaseQuery(database.database)
            val users = ExposedUserRepository(query)
            val organizations = ExposedOrganizationRepository(query)
            val roles = ExposedRoleRepository(query)
            val invitations = ExposedInvitationRepository(query)
            val members = ExposedMemberRepository(query)
            val now = Instant.now()
            val owner = testUser("transfer-owner", now)
            val successor = testUser("transfer-successor", now)
            val context = organizationContext(owner.id, "transfer-${UUID.randomUUID()}", now)
            users.create(owner)
            users.create(successor)
            organizations.createWithOwner(context)
            val organizationRoles = roles.list(context.organization.id)
            val ownerRole = organizationRoles.single { it.key == "owner" }
            val memberRole = organizationRoles.single { it.key == "member" }
            val invitation = OrganizationInvitation(
                UUID.randomUUID(), context.organization.id, context.organization.name, successor.email, memberRole,
                owner.id, "f".repeat(64), InvitationStatus.PENDING, now.plusSeconds(3600), now, now,
            )
            invitations.create(invitation)
            assertTrue(invitations.accept(invitation, successor.id, UUID.randomUUID(), now))
            val successorMembership = members.findByUserId(context.organization.id, successor.id)!!

            assertTrue(
                members.transferOwnership(
                    context.organization.id,
                    context.membership.id,
                    successorMembership.membershipId,
                    ownerRole.id,
                    memberRole.id,
                ),
            )
            val currentMembers = members.list(context.organization.id)
            assertEquals(1, currentMembers.count { it.role.key == "owner" && it.status == MembershipStatus.ACTIVE })
            assertEquals(successor.id, currentMembers.single { it.role.key == "owner" }.userId)

            assertFailsWith<Exception> {
                members.remove(context.organization.id, successorMembership.membershipId, now.plusSeconds(1))
            }
            assertEquals("owner", members.findByUserId(context.organization.id, successor.id)?.role?.key)
        }
    }

    @Test
    fun `audits persist safe events atomically and retain deleted resource history`() = runTest {
        DatabaseFactory.open(databaseConfig()).use { database ->
            val query = DatabaseQuery(database.database)
            val users = ExposedUserRepository(query)
            val organizations = ExposedOrganizationRepository(query)
            val roles = ExposedRoleRepository(query)
            val permissions = ExposedPermissionRepository(query)
            val invitations = ExposedInvitationRepository(query)
            val members = ExposedMemberRepository(query)
            val audits = ExposedAuditRepository(query)
            val authorization = AuthorizationService(ExposedAuthorizationRepository(query))
            val now = Instant.now()
            val owner = testUser("audit-owner", now)
            val invitee = testUser("audit-invitee", now)
            users.create(owner)
            users.create(invitee)
            val context = organizationContext(owner.id, "audit-${UUID.randomUUID()}", now)
            organizations.createWithOwner(context)
            val org = context.organization.id
            val organizationService = OrganizationService(organizations, authorization)
            organizationService.update(owner.id, UpdateOrganizationRequest(name = "Do not copy this secret into metadata"))
            val permissionService = PermissionService(authorization, permissions)
            val permission = permissionService.create(owner.id, CreatePermissionRequest("records.read", "Audit test", "sensitive description"))
            val permissionId = UUID.fromString(permission.id)
            permissionService.update(owner.id, permissionId, UpdatePermissionRequest(name = "Changed permission"))
            val roleService = RoleService(authorization, roles, permissions)
            val role = roleService.create(owner.id, CreateRoleRequest("auditor", "Auditor", "sensitive description"))
            val roleId = UUID.fromString(role.id)
            roleService.update(owner.id, roleId, UpdateRoleRequest(name = "Changed role"))
            roleService.replacePermissions(owner.id, roleId, ReplaceRolePermissionsRequest(listOf(permission.id)))
            val countBeforeNoOp = audits.list(org, AuditFilter(limit = 100)).data.size
            roleService.replacePermissions(owner.id, roleId, ReplaceRolePermissionsRequest(listOf(permission.id)))
            assertEquals(countBeforeNoOp, audits.list(org, AuditFilter(limit = 100)).data.size)
            roleService.replacePermissions(owner.id, roleId, ReplaceRolePermissionsRequest(emptyList()))
            val memberRole = roles.list(org).single { it.key == "member" }
            val invitation = OrganizationInvitation(UUID.randomUUID(), org, context.organization.name,
                invitee.email, memberRole, owner.id, UUID.randomUUID().toString().replace("-", "").repeat(2),
                InvitationStatus.PENDING, now.plusSeconds(3600), now, now)
            withAuditActor(owner.id) {
                invitations.create(invitation)
                invitations.renew(org, invitation.id, "r".repeat(64), now.plusSeconds(7200), now)
            }
            withAuditActor(invitee.id) { assertTrue(invitations.accept(invitation, invitee.id, UUID.randomUUID(), now)) }
            val member = members.findByUserId(org, invitee.id)!!
            val memberService = MemberService(authorization, members, roles)
            memberService.updateRole(owner.id, member.membershipId, UpdateMemberRoleRequest(role.id))
            memberService.remove(owner.id, member.membershipId)
            val revoked = invitation.copy(id = UUID.randomUUID(), tokenHash = "v".repeat(64))
            withAuditActor(owner.id) { invitations.create(revoked); invitations.revoke(org, revoked.id, now) }
            val unused = roleService.create(owner.id, CreateRoleRequest("unused", "Unused", null))
            roleService.delete(owner.id, UUID.fromString(unused.id))
            permissionService.delete(owner.id, permissionId)

            val events = audits.list(org, AuditFilter(limit = 100)).data
            val expected = setOf("organization.updated", "permission.created", "permission.updated", "permission.deleted",
                "role.created", "role.updated", "role.deleted", "role.permission_added", "role.permission_removed",
                "invitation.created", "invitation.resent", "invitation.accepted", "invitation.revoked", "member.role_updated", "member.removed")
            assertTrue(events.map { it.action }.containsAll(expected))
            assertEquals(invitee.id.toString(), events.single { it.action == "invitation.accepted" }.actorId)
            assertEquals(owner.id.toString(), events.single { it.action == "organization.updated" }.actorId)
            assertTrue(events.none { it.metadata.toString().contains("sensitive") || it.metadata.toString().contains("secret") || it.metadata.toString().contains(invitee.email) })
            val safeKeys = setOf(
                "nameChanged", "slugChanged", "keyChanged", "descriptionChanged", "userId", "previousRoleId",
                "roleId", "previousStatus", "status", "acceptedBy", "permissionId",
            )
            assertTrue(events.all { it.metadata.keys.all { key -> key in safeKeys } })
            assertTrue(audits.list(UUID.randomUUID(), AuditFilter()).data.isEmpty())
            val filtered = audits.list(org, AuditFilter(action = "member.removed", actorId = owner.id, resource = "member", resourceId = member.membershipId))
            assertEquals(1, filtered.data.size)
            val eventTime = Instant.parse(filtered.data.single().occurredAt)
            assertEquals(1, audits.list(org, AuditFilter(action = "member.removed", from = eventTime, to = eventTime)).data.size)
            val first = audits.list(org, AuditFilter(limit = 1))
            assertTrue(first.hasMore)
            assertEquals(events[1].id, audits.list(org, AuditFilter(limit = 1, offset = 1)).data.single().id)
            assertTrue(!audits.list(org, AuditFilter(offset = events.size.toLong())).hasMore)

            DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { connection ->
                for (sql in listOf("UPDATE organization_audit_logs SET action = 'tampered'", "DELETE FROM organization_audit_logs", "TRUNCATE organization_audit_logs")) {
                    assertFailsWith<SQLException> { connection.createStatement().use { it.execute(sql) } }
                }
                connection.autoCommit = false
                connection.createStatement().use { statement ->
                    statement.execute("UPDATE organizations SET name = 'Rolled back' WHERE id = '$org'")
                }
                connection.rollback()
            }
            assertEquals(events.size, audits.list(org, AuditFilter(limit = 100)).data.size)
            // Pool reuse must not retain the preceding actor.
            organizations.update(context.organization.copy(name = "Maintenance"))
            assertNull(audits.list(org, AuditFilter(limit = 1)).data.single().actorId)
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
