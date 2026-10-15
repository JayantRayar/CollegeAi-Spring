-- Stores individual chat conversations for each user.
CREATE TABLE chat_conversations (
                                    id BIGSERIAL PRIMARY KEY,
                                    user_id BIGINT NOT NULL,
                                    title VARCHAR(80) NOT NULL,
                                    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT fk_chat_conversations_user
                                        FOREIGN KEY (user_id)
                                            REFERENCES users(id)
                                            ON DELETE CASCADE
);

-- Stores user questions and assistant responses.
CREATE TABLE chat_messages (
                               id BIGSERIAL PRIMARY KEY,
                               conversation_id BIGINT NOT NULL,
                               role VARCHAR(20) NOT NULL,
                               content TEXT NOT NULL,
                               answer_type VARCHAR(50),
                               sources JSONB,
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT fk_chat_messages_conversation
                                   FOREIGN KEY (conversation_id)
                                       REFERENCES chat_conversations(id)
                                       ON DELETE CASCADE,

                               CONSTRAINT chk_chat_messages_role
                                   CHECK (role IN ('USER', 'ASSISTANT'))
);

-- Speeds up fetching conversations belonging to a user.
CREATE INDEX idx_chat_conversations_user_id
    ON chat_conversations(user_id);

-- Speeds up fetching messages belonging to a conversation.
CREATE INDEX idx_chat_messages_conversation_id
    ON chat_messages(conversation_id);

-- Keeps messages ordered efficiently within a conversation.
CREATE INDEX idx_chat_messages_conversation_created
    ON chat_messages(conversation_id, created_at, id);