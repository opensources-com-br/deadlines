CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    locale VARCHAR(16) NOT NULL DEFAULT 'pt-BR',
    timezone VARCHAR(64) NOT NULL DEFAULT 'America/Sao_Paulo',
    theme VARCHAR(16) NOT NULL DEFAULT 'system',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT user_preferences_locale_check CHECK (locale IN ('pt-BR', 'en')),
    CONSTRAINT user_preferences_theme_check CHECK (theme IN ('light', 'dark', 'system'))
);

INSERT INTO user_preferences (user_id)
SELECT id FROM users
ON CONFLICT (user_id) DO NOTHING;

CREATE FUNCTION create_default_user_preferences()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO user_preferences (user_id) VALUES (NEW.id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER users_create_default_preferences
AFTER INSERT ON users
FOR EACH ROW
EXECUTE FUNCTION create_default_user_preferences();
