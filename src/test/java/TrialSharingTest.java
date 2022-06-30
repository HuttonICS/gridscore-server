import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.*;
import jhi.gridscore.server.pojo.Configuration;
import jhi.gridscore.server.pojo.*;
import org.junit.jupiter.api.*;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TrialSharingTest extends ConfigTest
{
	private static Configuration trial;

	/**
	 * Create the initial configuration.
	 */
	@BeforeAll
	static void setUp()
		throws Exception
	{
		trial = new Configuration();
		trial.setName("Trial-test");
		trial.setRows(3);
		trial.setCols(1);
		trial.setTraits(Arrays.asList(
			new Trait().setName("t1").setType("categorical").setmType("single").setRestrictions(new Restrictions().setCategories(Arrays.asList("yellow", "white"))),
			new Trait().setName("t2").setType("date").setmType("single")
		));
		Cell[][] data = new Cell[3][1];
		data[0][0] = new Cell()
			.setValues(Arrays.asList(null, null))
			.setDates(Arrays.asList(null, null))
			.setName("g1");
		data[1][0] = new Cell()
			.setValues(Arrays.asList(null, null))
			.setDates(Arrays.asList(null, null))
			.setName("g2");
		data[2][0] = new Cell()
			.setValues(Arrays.asList(null, null))
			.setDates(Arrays.asList(null, null))
			.setName("g3");
		trial.setData(data);

		setUpClient();
	}

	/**
	 * Try sharing it initially.
	 */
	@Order(1)
	@Test
	void shareInitialConfig()
		throws Exception
	{
		ApiResult<Configuration> result = sendConfiguration(trial);
		Assertions.assertEquals(200, result.status);
		trial = result.data;
		Assertions.assertNotNull(trial.getUuid());
	}

	@Order(2)
	@Test
	void shareReallyOldConfig()
		throws Exception
	{
		Configuration old = new Configuration()
			.setRows(trial.getRows())
			.setCols(trial.getCols())
			.setName(trial.getName())
			.setDatasetType(trial.getDatasetType())
			.setBrapiConfig(trial.getBrapiConfig())
			.setUuid(trial.getUuid())
			.setComment(trial.getComment())
			.setTraits(trial.getTraits())
			.setCornerPoints(trial.getCornerPoints())
			.setMarkers(trial.getMarkers())
			.setLastUpdatedOn(trial.getLastUpdatedOn());
		Cell[][] data = new Cell[trial.getData().length][trial.getData()[0].length];
		Cell[][] oldData = trial.getData();
		for (int y = 0; y < data.length; y++)
		{
			for (int x = 0; x < data[y].length; x++)
			{
				data[y][x] = new Cell()
					.setName(oldData[y][x].getName())
					.setIsMarked(oldData[y][x].getIsMarked())
					.setGeolocation(oldData[y][x].getGeolocation())
					.setComment(oldData[y][x].getComment())
					.setValues(oldData[y][x].getValues())
					.setDates(oldData[y][x].getValues().stream().map(v -> v == null ? null : "1990-01-01").collect(Collectors.toList()));
			}
		}
		old.setData(data);

		ApiResult<Configuration> result = sendConfiguration(trial);
		Assertions.assertEquals(200, result.status);
		trial = result.data;
		Assertions.assertNotNull(trial.getUuid());

		old.setUuid(trial.getUuid());

		result = sendConfiguration(old);
		Assertions.assertEquals(200, result.status);
		assertConfigEquals(trial, result.data);
	}

	/**
	 * Send it again to make sure sending the same config multiple times works fine.
	 */
	@Order(3)
	@RepeatedTest(3)
	void testConfigEqualsAfterPostTwice()
		throws Exception
	{
		ApiResult<Configuration> result = sendConfiguration(trial);
		Assertions.assertEquals(200, result.status);
		assertConfigEquals(trial, result.data);
	}

	/**
	 * Attempt an actual change to the configuration. Check if changes are applied correctly.
	 */
	@Order(4)
	@Test
	void testConfigChangeConflict()
	{
		Cell cell = trial.getData()[0][0];
		cell.getValues().set(0, "yellow");
		cell.getDates().set(0, "2022-01-01");
		cell.setGeolocation(new Geolocation()
				.setLat(3d)
				.setLng(4d)
				.setElv(5d))
			.setComment("Test comment")
			.setIsMarked(true);

		ApiResult<Configuration> result = sendConfiguration(trial);
		Assertions.assertEquals(200, result.status);
		cell = result.data.getData()[0][0];
		Assertions.assertEquals("Test comment", cell.getComment());
		Assertions.assertNotNull(cell.getGeolocation());
		Assertions.assertEquals(3d, cell.getGeolocation().getLat());
		Assertions.assertEquals(4d, cell.getGeolocation().getLng());
		Assertions.assertEquals(5d, cell.getGeolocation().getElv());
		Assertions.assertTrue(cell.getIsMarked());
		Assertions.assertEquals("2022-01-01", cell.getDates().get(0));
		Assertions.assertEquals("yellow", cell.getValues().get(0));
		trial = result.data;
	}

	/**
	 * Tests the server's response to setting a new value for the same cell and same date. Server should respond with 428.
	 * Then save the data back twice, once using the THEIRS priority, then using the MINE priority.
	 */
	@Order(5)
	@Test
	void testConfigChangeSameDate()
	{
		// Set a new value without changing the date, this will result in an unresolvable conflict for the server -> 428 error.
		Cell cell = trial.getData()[0][0];
		cell.getValues().set(0, "white");
		ApiResult<Configuration> result = sendConfiguration(trial);
		Assertions.assertEquals(428, result.status);

		result = sendConfiguration(trial, "THEIRS");
		Assertions.assertEquals(200, result.status);
		Assertions.assertEquals("yellow", result.data.getData()[0][0].getValues().get(0));

		result = sendConfiguration(trial, "MINE");
		Assertions.assertEquals(200, result.status);
		Assertions.assertEquals("white", result.data.getData()[0][0].getValues().get(0));

		trial = result.data;
	}

	/**
	 * Test adding traits. Add one trait first, then send off, then remove that trait and add a different one, then expect 4 traits back.
	 */
	@Order(6)
	@Test
	void testAddTrait()
	{
		// Add a trait to the config
		trial.getTraits().add(new Trait().setName("t3").setType("int").setRestrictions(new Restrictions().setMin(0d).setMax(10d)));
		// Extend data and dates lists
		Arrays.stream(trial.getData())
			  .forEach(r -> {
				  Arrays.stream(r)
						.forEach(c -> {
							c.getValues().add(null);
							c.getDates().add(null);
						});
			  });
		// Set values for germplasm 3
		trial.getData()[2][0].getDates().set(2, "2022-02-02");
		trial.getData()[2][0].getValues().set(2, "7");

		// Send
		ApiResult<Configuration> result = sendConfiguration(trial);
		Assertions.assertEquals(200, result.status);
		assertConfigEquals(trial, result.data);

		// Now remove the trait
		trial.getTraits().remove(trial.getTraits().size() - 1);
		// Add a different one, now we still have 3 traits, just the third is different
		trial.getTraits().add(new Trait().setName("t4").setType("float").setRestrictions(new Restrictions().setMin(1d).setMax(3d)));
		// Update the values
		trial.getData()[2][0].getDates().set(2, "2022-02-01");
		trial.getData()[2][0].getValues().set(2, "3.5");

		// Send
		result = sendConfiguration(trial);
		Assertions.assertEquals(200, result.status);
		Configuration res = result.data;
		// The result should have 4 traits
		Assertions.assertEquals(4, res.getTraits().size());
		Assertions.assertEquals(4, res.getData()[0][0].getValues().size());
		// Check the values for trait 3 and 4 are correct for germplasm 3
		Assertions.assertEquals("7", res.getData()[2][0].getValues().get(2));
		Assertions.assertEquals("2022-02-02", res.getData()[2][0].getDates().get(2));
		Assertions.assertEquals("3.5", res.getData()[2][0].getValues().get(3));
		Assertions.assertEquals("2022-02-01", res.getData()[2][0].getDates().get(3));

		trial = result.data;
	}

	/**
	 * Test the GET call to make sure it returns the same (correct) data.
	 */
	@Order(7)
	@RepeatedTest(3)
	void testGetConfig()
	{
		ApiResult<Configuration> result = getConfiguration(trial.getUuid());
		Assertions.assertEquals(200, result.status);
		assertConfigEquals(trial, result.data);

		// Method not allowed for GET request without parameter
		result = getConfiguration(null);
		Assertions.assertEquals(405, result.status);

		// Not found for GET request with wrong parameter
		result = getConfiguration("something-something");
		Assertions.assertEquals(404, result.status);
	}

	@Order(8)
	@Test
	void testStructureChangeError()
	{
		trial.setRows(4);
		trial.setCols(2);
		ApiResult<Configuration> result = sendConfiguration(trial);
		Assertions.assertEquals(409, result.status);
		trial.setRows(trial.getData().length);
		trial.setCols(trial.getData()[0].length);
	}

	@Order(9)
	@Test
	void testCheckForUpdate()
	{
		// Check whether there's an update if you set the timestamp to something very long ago
		ApiResult<List<Boolean>> result = checkForUpdate(trial, new Timestamp(trial.getLastUpdatedOn().getTime() - 1000));
		Assertions.assertEquals(200, result.status);
		List<Boolean> data = result.data;
		Assertions.assertEquals(1, data.size());
		Assertions.assertEquals(true, data.get(0));

		// Send it again twice and assume that the response is always the same
		ApiResult<Configuration> configResult = sendConfiguration(trial);
		Assertions.assertEquals(200, configResult.status);
		assertConfigEquals(trial, configResult.data);
		Configuration temp = configResult.data;
		configResult = sendConfiguration(temp);
		Assertions.assertEquals(200, configResult.status);
		assertConfigEquals(temp, configResult.data);

		result = checkForUpdate(trial, trial.getLastUpdatedOn());
		Assertions.assertEquals(200, result.status);
		data = result.data;
		Assertions.assertEquals(1, data.size());
		Assertions.assertEquals(false, data.get(0));
	}

	/**
	 * Remove the configuration from the database again.
	 */
	@AfterAll
	static void breakDown()
		throws Exception
	{
		WebTarget target = client.target(URL)
								 .path(trial.getUuid())
								 .queryParam("name", trial.getName());

		Response resp = target.request(MediaType.APPLICATION_JSON)
							  .delete();

		Assertions.assertEquals(200, resp.getStatus());
	}
}
