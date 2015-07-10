-- helper ALIAS for executing sql queries with dynamic identifiers
CREATE ALIAS IF NOT EXISTS execute AS $$ void executeSql(Connection conn, String sql)
throws SQLException { conn.createStatement().executeUpdate(sql); } $$;

-- updates RUN table to use BASELINE_TREE's ID as a foreign key instead of an implicit (and incorrect) link to SUITE's ID
ALTER TABLE run ADD COLUMN baseline_tree_id INTEGER AFTER branch_name;
UPDATE run SET baseline_tree_id = run.suite_id;

CALL execute('ALTER TABLE run DROP CONSTRAINT ' ||
    (SELECT DISTINCT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.CONSTRAINTS WHERE TABLE_NAME = 'RUN' AND COLUMN_LIST='SUITE_ID,BRANCH_NAME'));

ALTER TABLE run ADD CONSTRAINT RUN_CONSTRAINT_FOR_1 FOREIGN KEY(baseline_tree_id, branch_name) INDEX CONSTRAINT_INDEX_RUN_FOR_1 REFERENCES baseline_branch(baseline_tree, name) ON DELETE CASCADE NOCHECK


