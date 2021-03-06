CREATE TABLE "RULES_PROFILES" (
  "ID" INTEGER NOT NULL GENERATED BY DEFAULT AS IDENTITY (START WITH 1, INCREMENT BY 1),
  "NAME" VARCHAR(100) NOT NULL,
  "LANGUAGE" VARCHAR(20),
  "PARENT_NAME" VARCHAR(255),

  "KEE" VARCHAR(100),
  "PARENT_KEE" VARCHAR(255),
  "RULES_UPDATED_AT" VARCHAR(100),
  "CREATED_AT" TIMESTAMP,
  "UPDATED_AT" TIMESTAMP
);
