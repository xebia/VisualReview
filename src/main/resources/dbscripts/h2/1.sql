-- CREATE DATABASE visualreview
CREATE TABLE IF NOT EXISTS project
(
  id   BIGINT AUTO_INCREMENT CONSTRAINT project_pk PRIMARY KEY,
  name VARCHAR UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS suite
(
  id         BIGINT AUTO_INCREMENT CONSTRAINT suite_pk PRIMARY KEY,
  project_id INTEGER NOT NULL REFERENCES project,
  name       VARCHAR NOT NULL,
  UNIQUE (project_id, name)
);

CREATE TABLE IF NOT EXISTS run
(
  id         BIGINT AUTO_INCREMENT CONSTRAINT run_pk PRIMARY KEY,
  suite_id   INTEGER REFERENCES suite,
  start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  end_time   TIMESTAMP,
  status     VARCHAR(31) NOT NULL
);

CREATE TABLE IF NOT EXISTS screenshot
(
  id              BIGINT AUTO_INCREMENT CONSTRAINT screenshot_pk PRIMARY KEY,
  size            INTEGER     NOT NULL,
  resolution      VARCHAR(31) NOT NULL,
  os              VARCHAR(31) NOT NULL,
  browser         VARCHAR(31) NOT NULL,
  version         VARCHAR(31) NOT NULL,
  screenshot_name VARCHAR     NOT NULL,
  run_id          INTEGER     NOT NULL REFERENCES run,
  path            VARCHAR     NOT NULL,
  UNIQUE (run_id, screenshot_name)
);

CREATE TABLE IF NOT EXISTS baseline
(
  id       BIGINT AUTO_INCREMENT CONSTRAINT baseline_pk PRIMARY KEY,
  suite_id INTEGER UNIQUE REFERENCES suite
);

CREATE TABLE IF NOT EXISTS baseline_screenshot
(
  baseline_id   BIGINT NOT NULL REFERENCES baseline,
  screenshot_id BIGINT NOT NULL REFERENCES screenshot,
  UNIQUE (baseline_id, screenshot_id)
);

CREATE TABLE IF NOT EXISTS analysis
(
  id            BIGINT AUTO_INCREMENT CONSTRAINT analysis_pk PRIMARY KEY,
  creation_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  run_id        INTEGER UNIQUE           NOT NULL REFERENCES run,
  baseline_id   INTEGER                  NOT NULL REFERENCES baseline
);

CREATE TABLE IF NOT EXISTS diff_image
(
  id   BIGINT AUTO_INCREMENT CONSTRAINT diff_image_pk PRIMARY KEY,
  path VARCHAR NOT NULL
);

CREATE TABLE IF NOT EXISTS diff
(
  id          BIGINT AUTO_INCREMENT CONSTRAINT diff_pk PRIMARY KEY,
  before      INTEGER        NOT NULL REFERENCES screenshot,
  after       INTEGER        NOT NULL REFERENCES screenshot,
  percentage  NUMERIC(5, 2)  NOT NULL,
  diff_image  INTEGER UNIQUE NOT NULL REFERENCES diff_image,
  status      VARCHAR(8)     NOT NULL,
  analysis_id INTEGER        NOT NULL REFERENCES analysis
);

