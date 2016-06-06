-- CREATE DATABASE visualreview

CREATE TABLE IF NOT EXISTS image
(
  id        BIGINT AUTO_INCREMENT CONSTRAINT image_pk PRIMARY KEY,
  directory VARCHAR(250) NOT NULL
);

CREATE TABLE IF NOT EXISTS project
(
  id   BIGINT AUTO_INCREMENT CONSTRAINT project_pk PRIMARY KEY,
  name VARCHAR UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS suite
(
  id         BIGINT AUTO_INCREMENT CONSTRAINT suite_pk PRIMARY KEY,
  project_id INTEGER NOT NULL REFERENCES project ON DELETE CASCADE,
  name       VARCHAR NOT NULL,
  UNIQUE (project_id, name)
);

CREATE TABLE IF NOT EXISTS baseline_node
(
  id     BIGINT AUTO_INCREMENT CONSTRAINT baseline_node_pk PRIMARY KEY,
  parent BIGINT REFERENCES baseline_node,
  CHECK (id <> parent)
);

CREATE TABLE IF NOT EXISTS baseline_tree
(
  id            INTEGER AUTO_INCREMENT PRIMARY KEY,
  suite_id      BIGINT NOT NULL UNIQUE REFERENCES suite ON DELETE CASCADE,
  baseline_root BIGINT NOT NULL UNIQUE REFERENCES baseline_node ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS baseline_branch
(
  name          VARCHAR(32),
  baseline_tree INTEGER REFERENCES baseline_tree ON DELETE CASCADE,
  branch_root   BIGINT NOT NULL REFERENCES baseline_node,
  head          BIGINT NOT NULL UNIQUE REFERENCES baseline_node,
  PRIMARY KEY (baseline_tree, name)
);
CREATE UNIQUE INDEX IF NOT EXISTS branch_head ON baseline_branch (head);

CREATE TABLE IF NOT EXISTS run
(
  id          BIGINT AUTO_INCREMENT CONSTRAINT run_pk PRIMARY KEY,
  suite_id    INTEGER,
  branch_name VARCHAR(32),
  start_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  end_time    TIMESTAMP,
  status      VARCHAR(31) NOT NULL,
  FOREIGN KEY (suite_id, branch_name) REFERENCES baseline_branch ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS screenshot
(
  id              BIGINT AUTO_INCREMENT CONSTRAINT screenshot_pk PRIMARY KEY,
  size            INTEGER      NOT NULL,
  properties      VARCHAR(512) NOT NULL,
  meta            VARCHAR(512) NOT NULL,
  screenshot_name VARCHAR      NOT NULL,
  run_id          INTEGER      NOT NULL REFERENCES run ON DELETE CASCADE,
  image_id        BIGINT       UNIQUE REFERENCES image,
  UNIQUE (run_id, screenshot_name, properties)
);

CREATE TABLE IF NOT EXISTS bl_node_screenshot
(
  baseline_node BIGINT NOT NULL REFERENCES baseline_node ON DELETE CASCADE,
  screenshot_id BIGINT NOT NULL REFERENCES screenshot ON DELETE CASCADE,
  UNIQUE (baseline_node, screenshot_id)
);

CREATE TABLE IF NOT EXISTS analysis
(
  id            BIGINT AUTO_INCREMENT CONSTRAINT analysis_pk PRIMARY KEY,
  creation_time TIMESTAMP                NOT NULL DEFAULT CURRENT_TIMESTAMP,
  run_id        INTEGER UNIQUE           NOT NULL REFERENCES run ON DELETE CASCADE,
  baseline_node INTEGER                  NOT NULL REFERENCES baseline_node ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS diff
(
  id          BIGINT AUTO_INCREMENT CONSTRAINT diff_pk PRIMARY KEY,
  before      INTEGER        REFERENCES screenshot ON DELETE CASCADE,
  after       INTEGER        NOT NULL REFERENCES screenshot ON DELETE CASCADE,
  percentage  NUMERIC(5, 2)  NOT NULL,
  image_id        BIGINT       UNIQUE REFERENCES image,
  mask_image_id        BIGINT       UNIQUE REFERENCES image,
  status      VARCHAR(8)     NOT NULL,
  analysis_id INTEGER        NOT NULL REFERENCES analysis ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS system
(
  skey        VARCHAR(100) NOT NULL,
  svalue      VARCHAR(200) NOT NULL,
  UNIQUE (skey)
);

-- set initial system key for schema version. Further .sql scripts don't have to do this
insert into SYSTEM (skey, svalue) VALUES ('schema_version', 1);
