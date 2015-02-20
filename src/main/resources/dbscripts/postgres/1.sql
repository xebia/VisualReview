-- CREATE DATABASE visualreview
CREATE TABLE project
(
  id   SERIAL CONSTRAINT project_pk PRIMARY KEY,
  name VARCHAR UNIQUE NOT NULL
);

CREATE TABLE suite
(
  id         SERIAL CONSTRAINT suite_pk PRIMARY KEY,
  project_id INTEGER NOT NULL REFERENCES project,
  name       VARCHAR NOT NULL,
  UNIQUE (project_id, name)
);

CREATE TABLE run
(
  id         SERIAL CONSTRAINT run_pk PRIMARY KEY,
  suite_id   INTEGER REFERENCES suite,
  start_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  end_time   TIMESTAMP WITH TIME ZONE,
  status     VARCHAR(31) NOT NULL
);

CREATE TABLE screenshot
(
  id              SERIAL CONSTRAINT screenshot_pk PRIMARY KEY,
  size            INTEGER      NOT NULL,
  meta            VARCHAR(512) NOT NULL,
  properties      VARCHAR(512) NOT NULL,
  screenshot_name VARCHAR      NOT NULL,
  run_id          INTEGER      NOT NULL REFERENCES run,
  path            VARCHAR      NOT NULL,
  UNIQUE (run_id, screenshot_name)
);

CREATE TABLE baseline
(
  id       SERIAL CONSTRAINT baseline_pk PRIMARY KEY,
  suite_id INTEGER UNIQUE REFERENCES suite
);

CREATE TABLE baseline_screenshot
(
  baseline_id   INTEGER NOT NULL REFERENCES baseline,
  screenshot_id INTEGER NOT NULL REFERENCES screenshot,
  UNIQUE (baseline_id, screenshot_id)
);

CREATE TABLE analysis
(
  id            SERIAL CONSTRAINT analysis_pk PRIMARY KEY,
  creation_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  run_id        INTEGER UNIQUE           NOT NULL REFERENCES run,
  baseline_id   INTEGER                  NOT NULL REFERENCES baseline
);

CREATE TABLE diff_image
(
  id   SERIAL CONSTRAINT diff_image_pk PRIMARY KEY,
  path VARCHAR NOT NULL
);

CREATE TABLE diff
(
  id          SERIAL CONSTRAINT diff_pk PRIMARY KEY,
  before      INTEGER        NOT NULL REFERENCES screenshot,
  after       INTEGER        NOT NULL REFERENCES screenshot,
  percentage  NUMERIC(5, 2)  NOT NULL,
  diff_image  INTEGER UNIQUE NOT NULL REFERENCES diff_image,
  status      VARCHAR(8)     NOT NULL,
  analysis_id INTEGER        NOT NULL REFERENCES analysis
);

