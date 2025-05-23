package ca.uhn.fhir.jpa.migrate.taskdef;

import ca.uhn.fhir.jpa.migrate.DriverTypeEnum;
import ca.uhn.fhir.jpa.migrate.HapiMigrationException;
import ca.uhn.fhir.jpa.migrate.JdbcUtils;
import ca.uhn.fhir.jpa.migrate.entity.HapiMigrationEntity;
import ca.uhn.fhir.jpa.migrate.tasks.api.TaskFlagEnum;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


@SuppressWarnings("SqlDialectInspection")
public class ModifyColumnTest extends BaseTest {

	@ParameterizedTest(name = "{index}: {0}")
	@MethodSource("data")
	public void testColumnWithJdbcTypeClob(Supplier<TestDatabaseDetails> theTestDatabaseDetails) throws SQLException {
		before(theTestDatabaseDetails);

		if (getDriverType() == DriverTypeEnum.DERBY_EMBEDDED) {
			return;
		}

		executeSql("create table SOMETABLE (TEXTCOL clob)");

		ModifyColumnTask task = new ModifyColumnTask("1", "1");
		task.setTableName("SOMETABLE");
		task.setColumnName("TEXTCOL");
		task.setColumnType(ColumnTypeEnum.STRING);
		task.setNullable(true);
		task.setColumnLength(250);
		getMigrator().addTask(task);

		getMigrator().migrate();

		assertEquals(new JdbcUtils.ColumnType(ColumnTypeEnum.STRING, 250), JdbcUtils.getColumnType(getConnectionProperties(), "SOMETABLE", "TEXTCOL"));
		assertThat(task.getExecutedStatements()).hasSize(1);

		// Make sure additional migrations don't crash
		getMigrator().migrate();
		getMigrator().migrate();

	}

	@ParameterizedTest(name = "{index}: {0}")
	@MethodSource("data")
	public void testColumnAlreadyExists(Supplier<TestDatabaseDetails> theTestDatabaseDetails) throws SQLException {
		before(theTestDatabaseDetails);

		executeSql("create table SOMETABLE (PID bigint not null, TEXTCOL varchar(255), newcol bigint)");

		ModifyColumnTask task = new ModifyColumnTask("1", "1");
		task.setTableName("SOMETABLE");
		task.setColumnName("TEXTCOL");
		task.setColumnType(ColumnTypeEnum.STRING);
		task.setNullable(true);
		task.setColumnLength(300);
		getMigrator().addTask(task);

		getMigrator().migrate();

		assertEquals(new JdbcUtils.ColumnType(ColumnTypeEnum.STRING, 300), JdbcUtils.getColumnType(getConnectionProperties(), "SOMETABLE", "TEXTCOL"));
		assertThat(task.getExecutedStatements()).hasSize(1);

		// Make sure additional migrations don't crash
		getMigrator().migrate();
		getMigrator().migrate();

	}

	@ParameterizedTest(name = "{index}: {0}")
	@MethodSource("data")
	public void testNoShrink_SameNullable(Supplier<TestDatabaseDetails> theTestDatabaseDetails) throws SQLException {
		before(theTestDatabaseDetails);

		executeSql("create table SOMETABLE (PID bigint not null, TEXTCOL varchar(255), newcol bigint)");

		ModifyColumnTask task = new ModifyColumnTask("1", "123456.7");
		task.setTableName("SOMETABLE");
		task.setColumnName("TEXTCOL");
		task.setColumnType(ColumnTypeEnum.STRING);
		task.setNullable(true);
		task.setColumnLength(200);

		getMigrator().setNoColumnShrink(true);
		getMigrator().addTask(task);
		getMigrator().migrate();

		assertThat(task.getExecutedStatements()).isEmpty();
		assertEquals(new JdbcUtils.ColumnType(ColumnTypeEnum.STRING, 255), JdbcUtils.getColumnType(getConnectionProperties(), "SOMETABLE", "TEXTCOL"));

		// Make sure additional migrations don't crash
		getMigrator().migrate();
		getMigrator().migrate();

	}

	@ParameterizedTest(name = "{index}: {0}")
	@MethodSource("data")
	public void testColumnMakeNullable(Supplier<TestDatabaseDetails> theTestDatabaseDetails) throws SQLException {
		before(theTestDatabaseDetails);

		executeSql("create table SOMETABLE (PID bigint not null, TEXTCOL varchar(255) not null)");
		assertFalse(JdbcUtils.isColumnNullable(getConnectionProperties(), "SOMETABLE", "PID"));
		assertFalse(JdbcUtils.isColumnNullable(getConnectionProperties(), "SOMETABLE", "TEXTCOL"));
		assertEquals(getLongColumnType(theTestDatabaseDetails), JdbcUtils.getColumnType(getConnectionProperties(), "SOMETABLE", "PID"));
		assertEquals(new JdbcUtils.ColumnType(ColumnTypeEnum.STRING, 255), JdbcUtils.getColumnType(getConnectionProperties(), "SOMETABLE", "TEXTCOL"));

		// PID
		ModifyColumnTask task = new ModifyColumnTask("1", "1");
		task.setTableName("SOMETABLE");
		task.setColumnName("PID");
		task.setColumnType(ColumnTypeEnum.LONG);
		task.setNullable(true);
		getMigrator().addTask(task);

		// STRING
		task = new ModifyColumnTask("1", "2");
		task.setTableName("SOMETABLE");
		task.setColumnName("TEXTCOL");
		task.setColumnType(ColumnTypeEnum.STRING);
		task.setNullable(true);
		task.setColumnLength(255);
		getMigrator().addTask(task);

		// Do migration
		getMigrator().migrate();

		assertTrue(JdbcUtils.isColumnNullable(getConnectionProperties(), "SOMETABLE", "PID"));
		assertTrue(JdbcUtils.isColumnNullable(getConnectionProperties(), "SOMETABLE", "TEXTCOL"));
		assertEquals(getLongColumnType(theTestDatabaseDetails), JdbcUtils.getColumnType(getConnectionProperties(), "SOMETABLE", "PID"));
		assertEquals(new JdbcUtils.ColumnType(ColumnTypeEnum.STRING, 255), JdbcUtils.getColumnType(getConnectionProperties(), "SOMETABLE", "TEXTCOL"));

		// Make sure additional migrations don't crash
		getMigrator().migrate();
		getMigrator().migrate();


	}

	@ParameterizedTest(name = "{index}: {0}")
	@MethodSource("data")
	public void testNoShrink_ColumnMakeDateNullable(Supplier<TestDatabaseDetails> theTestDatabaseDetails) throws SQLException {
		before(theTestDatabaseDetails);

		executeSql("create table SOMETABLE (PID bigint not null, DATECOL timestamp not null)");
		assertFalse(JdbcUtils.isColumnNullable(getConnectionProperties(), "SOMETABLE", "PID"));
		assertFalse(JdbcUtils.isColumnNullable(getConnectionProperties(), "SOMETABLE", "DATECOL"));
		assertEquals(getLongColumnType(theTestDatabaseDetails), JdbcUtils.getColumnType(getConnectionProperties(), "SOMETABLE", "PID"));
		assertEquals(ColumnTypeEnum.DATE_TIMESTAMP, JdbcUtils.getColumnType(getConnectionProperties(), "SOMETABLE", "DATECOL").getColumnTypeEnum());

		getMigrator().setNoColumnShrink(true);

		// PID
		ModifyColumnTask task = new ModifyColumnTask("1", "1");
		task.setTableName("SOMETABLE");
		task.setColumnName("PID");
		task.setColumnType(ColumnTypeEnum.LONG);
		task.setNullable(true);
		getMigrator().addTask(task);

		// STRING
		task = new ModifyColumnTask("1", "2");
		task.setTableName("SOMETABLE");
		task.setColumnName("DATECOL");
		task.setColumnType(ColumnTypeEnum.DATE_TIMESTAMP);
		task.setNullable(true);
		getMigrator().addTask(task);

		// Do migration
		getMigrator().migrate();

		assertTrue(JdbcUtils.isColumnNullable(getConnectionProperties(), "SOMETABLE", "PID"));
		assertTrue(JdbcUtils.isColumnNullable(getConnectionProperties(), "SOMETABLE", "DATECOL"));
		assertEquals(getLongColumnType(theTestDatabaseDetails), JdbcUtils.getColumnType(getConnectionProperties(), "SOMETABLE", "PID"));
		assertEquals(ColumnTypeEnum.DATE_TIMESTAMP, JdbcUtils.getColumnType(getConnectionProperties(), "SOMETABLE", "DATECOL").getColumnTypeEnum());

		// Make sure additional migrations don't crash
		getMigrator().migrate();
		getMigrator().migrate();
	}

	@ParameterizedTest(name = "{index}: {0}")
	@MethodSource("data")
	public void testColumnMakeNotNullable(Supplier<TestDatabaseDetails> theTestDatabaseDetails) throws SQLException {
		before(theTestDatabaseDetails);

		executeSql("create table SOMETABLE (PID bigint, TEXTCOL varchar(255))");
		assertTrue(JdbcUtils.isColumnNullable(getConnectionProperties(), "SOMETABLE", "PID"));
		assertTrue(JdbcUtils.isColumnNullable(getConnectionProperties(), "SOMETABLE", "TEXTCOL"));
		assertEquals(getLongColumnType(theTestDatabaseDetails), JdbcUtils.getColumnType(getConnectionProperties(), "SOMETABLE", "PID"));
		assertEquals(new JdbcUtils.ColumnType(ColumnTypeEnum.STRING, 255), JdbcUtils.getColumnType(getConnectionProperties(), "SOMETABLE", "TEXTCOL"));

		// PID
		ModifyColumnTask task = new ModifyColumnTask("1", "1");
		task.setTableName("SOMETABLE");
		task.setColumnName("PID");
		task.setColumnType(ColumnTypeEnum.LONG);
		task.setNullable(false);
		getMigrator().addTask(task);

		// STRING
		task = new ModifyColumnTask("1", "2");
		task.setTableName("SOMETABLE");
		task.setColumnName("TEXTCOL");
		task.setColumnType(ColumnTypeEnum.STRING);
		task.setNullable(false);
		task.setColumnLength(255);
		getMigrator().addTask(task);

		// Do migration
		getMigrator().migrate();

		assertFalse(JdbcUtils.isColumnNullable(getConnectionProperties(), "SOMETABLE", "PID"));
		assertFalse(JdbcUtils.isColumnNullable(getConnectionProperties(), "SOMETABLE", "TEXTCOL"));
		assertEquals(getLongColumnType(theTestDatabaseDetails), JdbcUtils.getColumnType(getConnectionProperties(), "SOMETABLE", "PID"));
		assertEquals(new JdbcUtils.ColumnType(ColumnTypeEnum.STRING, 255), JdbcUtils.getColumnType(getConnectionProperties(), "SOMETABLE", "TEXTCOL"));

		// Make sure additional migrations don't crash
		getMigrator().migrate();
		getMigrator().migrate();

	}

	@SuppressWarnings("EnumSwitchStatementWhichMissesCases")
	@Nonnull
	private JdbcUtils.ColumnType getLongColumnType(Supplier<TestDatabaseDetails> theTestDatabaseDetails) {
		return switch (theTestDatabaseDetails.get().getDriverType()) {
			case H2_EMBEDDED -> new JdbcUtils.ColumnType(ColumnTypeEnum.LONG, 64);
			case DERBY_EMBEDDED -> new JdbcUtils.ColumnType(ColumnTypeEnum.LONG, 19);
			default -> throw new UnsupportedOperationException();
		};
	}

	@ParameterizedTest(name = "{index}: {0}")
	@MethodSource("data")
	public void testColumnDoesntAlreadyExist(Supplier<TestDatabaseDetails> theTestDatabaseDetails) throws SQLException {
		before(theTestDatabaseDetails);

		executeSql("create table SOMETABLE (PID bigint, TEXTCOL varchar(255))");

		ModifyColumnTask task = new ModifyColumnTask("1", "1");
		task.setTableName("SOMETABLE");
		task.setColumnName("SOMECOLUMN");
		task.setDescription("Make nullable");
		task.setNullable(true);
		getMigrator().addTask(task);

		getMigrator().migrate();

		assertThat(JdbcUtils.getColumnNames(getConnectionProperties(), "SOMETABLE")).containsExactlyInAnyOrder("PID", "TEXTCOL");
	}

	@ParameterizedTest(name = "{index}: {0}")
	@MethodSource("data")
	public void testFailureAllowed(Supplier<TestDatabaseDetails> theTestDatabaseDetails) throws SQLException {
		before(theTestDatabaseDetails);

		executeSql("create table SOMETABLE (PID bigint, TEXTCOL varchar(255))");
		executeSql("insert into SOMETABLE (TEXTCOL) values ('HELLO')");

		ModifyColumnTask task = new ModifyColumnTask("1", "1");
		task.addFlag(TaskFlagEnum.FAILURE_ALLOWED);
		task.setTableName("SOMETABLE");
		task.setColumnName("TEXTCOL");
		task.setColumnType(ColumnTypeEnum.LONG);
		task.setNullable(true);
		getMigrator().addTask(task);

		getMigrator().migrate();
		assertEquals(ColumnTypeEnum.STRING, JdbcUtils.getColumnType(getConnectionProperties(), "SOMETABLE", "TEXTCOL").getColumnTypeEnum());

		List<HapiMigrationEntity> entities = myHapiMigrationDao.findAll();
		assertThat(entities).hasSize(1);
		assertThat(entities.get(0).getResult()).isEqualTo(MigrationTaskExecutionResultEnum.NOT_APPLIED_ALLOWED_FAILURE.name());
	}

	@ParameterizedTest(name = "{index}: {0}")
	@MethodSource("data")
	public void testFailureNotAllowed(Supplier<TestDatabaseDetails> theTestDatabaseDetails) {
		before(theTestDatabaseDetails);

		executeSql("create table SOMETABLE (PID bigint, TEXTCOL varchar(255))");
		executeSql("insert into SOMETABLE (TEXTCOL) values ('HELLO')");

		ModifyColumnTask task = new ModifyColumnTask("1", "1");
		task.setTableName("SOMETABLE");
		task.setColumnName("TEXTCOL");
		task.setColumnType(ColumnTypeEnum.LONG);
		task.setNullable(true);
		getMigrator().addTask(task);

		try {
			getMigrator().migrate();
			fail();
		} catch (HapiMigrationException e) {
			// expected
		}

	}

	@ParameterizedTest(name = "{index}: {0}")
	@MethodSource("data")
	public void dontCompareLengthIfNoneSpecifiedInTask(Supplier<TestDatabaseDetails> theTestDatabaseDetails) throws SQLException {
		before(theTestDatabaseDetails);

		executeSql("create table SOMETABLE (PID bigint, TEXTCOL varchar(255))");

		ModifyColumnTask task = new ModifyColumnTask("1", "1");
		task.setTableName("SOMETABLE");
		task.setColumnName("PID");
		task.setColumnType(ColumnTypeEnum.LONG);
		task.setNullable(true);

		JdbcUtils.ColumnType existingColumnType = JdbcUtils.getColumnType(getConnectionProperties(), "SOMETABLE", "PID");
		assertEquals(getLongColumnType(theTestDatabaseDetails), existingColumnType);
		assertTrue(existingColumnType.equals(task.getColumnType(), task.getColumnLength()));
	}


	@ParameterizedTest(name = "{index}: {0}")
	@MethodSource("data")
	public void testShrinkDoesntFailIfShrinkCannotProceed(Supplier<TestDatabaseDetails> theTestDatabaseDetails) throws SQLException {
		before(theTestDatabaseDetails);

		executeSql("create table SOMETABLE (PID bigint not null, TEXTCOL varchar(10))");
		executeSql("insert into SOMETABLE (PID, TEXTCOL) values (1, '0123456789')");

		ModifyColumnTask task = new ModifyColumnTask("1", "123456.7");
		task.setTableName("SOMETABLE");
		task.setColumnName("TEXTCOL");
		task.setColumnType(ColumnTypeEnum.STRING);
		task.setNullable(true);
		task.setColumnLength(5);

		getMigrator().addTask(task);
		getMigrator().migrate();

		assertThat(task.getExecutedStatements()).hasSize(1);
		assertEquals(new JdbcUtils.ColumnType(ColumnTypeEnum.STRING, 10), JdbcUtils.getColumnType(getConnectionProperties(), "SOMETABLE", "TEXTCOL"));

		// Make sure additional migrations don't crash
		getMigrator().migrate();
		getMigrator().migrate();
	}

	@Nested
	public class SqlFeatures{

		@Test
		public void testIncreaseColumnSize_onOracleDb_willIncludeColumnSemantic() throws SQLException {
			// given
			ModifyColumnTask task = new ModifyColumnTask("1", "123456.7");
			task.setTableName("SOMETABLE");
			task.setColumnName("SOMECOLUMN");
			task.setColumnType(ColumnTypeEnum.STRING);
			task.setColumnLength(200);
			task.setNullable(true);
			task.setDriverType(DriverTypeEnum.ORACLE_12C);

			// this is the definition of the column before the migration
			JdbcUtils.ColumnType columnType = new JdbcUtils.ColumnType(ColumnTypeEnum.STRING, 100);

			// when
			List<String> sqlStringsToExecute = task.generateSql(columnType, true);

			// then
			assertThat(sqlStringsToExecute.get(0)).isEqualTo("alter table SOMETABLE modify ( SOMECOLUMN varchar2(200 char)  )");
		}
	}
}
