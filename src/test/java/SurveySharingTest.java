import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.*;
import jhi.gridscore.server.pojo.Configuration;
import jhi.gridscore.server.pojo.*;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.junit.jupiter.api.*;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SurveySharingTest extends ConfigTest
{
	private static Configuration      survey;

	/**
	 * Create the initial configuration.
	 */
	@BeforeAll
	static void setUp()
		throws Exception
	{
		survey = new Configuration();
		survey.setName("Survey-test");
		survey.setDatasetType(DatasetType.SURVEY);
		survey.setRows(3);
		survey.setCols(1);
		survey.setTraits(Arrays.asList(
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
		survey.setData(data);

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
		ApiResult<Configuration> result = sendConfiguration(survey);
		Assertions.assertEquals(200, result.status);
		Assertions.assertNotNull(result.data.getUuid());
		survey = result.data;
	}

	@Order(2)
	@Test
	void shareReallyOldConfig()
		throws Exception
	{
		Configuration old = new Configuration()
			.setRows(survey.getRows())
			.setCols(survey.getCols())
			.setName(survey.getName())
			.setDatasetType(survey.getDatasetType())
			.setBrapiConfig(survey.getBrapiConfig())
			.setUuid(survey.getUuid())
			.setComment(survey.getComment())
			.setTraits(survey.getTraits())
			.setCornerPoints(survey.getCornerPoints())
			.setMarkers(survey.getMarkers())
			.setLastUpdatedOn(survey.getLastUpdatedOn());
		Cell[][] data = new Cell[survey.getData().length][survey.getData()[0].length];
		Cell[][] oldData = survey.getData();
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

		ApiResult<Configuration> result = sendConfiguration(survey);
		Assertions.assertEquals(200, result.status);
		Assertions.assertNotNull(result.data.getUuid());
		survey = result.data;

		old.setUuid(survey.getUuid());

		result = sendConfiguration(old);
		Assertions.assertEquals(200, result.status);
		assertConfigEquals(survey, result.data);
	}

	/**
	 * Send it again to make sure sending the same config multiple times works fine.
	 */
	@Order(3)
	@RepeatedTest(3)
	void testConfigEqualsAfterPostTwice()
		throws Exception
	{
		ApiResult<Configuration> result = sendConfiguration(survey);
		Assertions.assertEquals(200, result.status);
		assertConfigEquals(survey, result.data);
	}

	/**
	 * Attempt an actual change to the configuration. Check if changes are applied correctly.
	 */
	@Order(4)
	@Test
	void testConfigChangeConflict()
	{
		Cell cell = survey.getData()[0][0];
		cell.getValues().set(0, "yellow");
		cell.getDates().set(0, "2022-01-01");
		cell.setGeolocation(new Geolocation()
				.setLat(3d)
				.setLng(4d)
				.setElv(5d))
			.setComment("Test comment")
			.setIsMarked(true);

		ApiResult<Configuration> result = sendConfiguration(survey);
		Assertions.assertEquals(200,  result.status);
		cell = result.data.getData()[0][0];

		Assertions.assertEquals("Test comment", cell.getComment());
		Assertions.assertNotNull(cell.getGeolocation());
		Assertions.assertEquals(3d, cell.getGeolocation().getLat());
		Assertions.assertEquals(4d, cell.getGeolocation().getLng());
		Assertions.assertEquals(5d, cell.getGeolocation().getElv());
		Assertions.assertTrue(cell.getIsMarked());
		Assertions.assertEquals("2022-01-01", cell.getDates().get(0));
		Assertions.assertEquals("yellow", cell.getValues().get(0));

		survey = result.data;
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
		Cell cell = survey.getData()[0][0];
		cell.getValues().set(0, "white");

		ApiResult<Configuration> result = sendConfiguration(survey);
		Assertions.assertEquals(428, result.status);

		// Now save it with THEIRS priority to ignore conflicting changes in our data
		result = sendConfiguration(survey, "THEIRS");
		Assertions.assertEquals(200, result.status);
		Assertions.assertEquals("yellow", result.data.getData()[0][0].getValues().get(0));

		// Now save it with MINE priority to override conflicting changes in their data
		result = sendConfiguration(survey, "MINE");
		Assertions.assertEquals(200, result.status);
		Assertions.assertEquals("white", result.data.getData()[0][0].getValues().get(0));

		survey = result.data;
	}

	/**
	 * Test adding traits. Add one trait first, then send off, then remove that trait and add a different one, then expect 4 traits back.
	 */
	@Order(6)
	@Test
	void testAddTrait()
	{
		// Add a trait to the config
		survey.getTraits().add(new Trait().setName("t3").setType("int").setRestrictions(new Restrictions().setMin(0d).setMax(10d)));
		// Extend data and dates lists
		Arrays.stream(survey.getData())
			  .forEach(r -> {
				  Arrays.stream(r)
						.forEach(c -> {
							c.getValues().add(null);
							c.getDates().add(null);
						});
			  });
		// Set values for germplasm 3
		survey.getData()[2][0].getDates().set(2, "2022-02-02");
		survey.getData()[2][0].getValues().set(2, "7");

		// Send
		ApiResult<Configuration> result = sendConfiguration(survey);
		Assertions.assertEquals(200, result.status);
		assertConfigEquals(survey, result.data);

		// Now remove the trait
		survey.getTraits().remove(survey.getTraits().size() - 1);
		// Add a different one, now we still have 3 traits, just the third is different
		survey.getTraits().add(new Trait().setName("t4").setType("float").setRestrictions(new Restrictions().setMin(1d).setMax(3d)));
		// Update the values
		survey.getData()[2][0].getDates().set(2, "2022-02-01");
		survey.getData()[2][0].getValues().set(2, "3.5");

		// Send
		result = sendConfiguration(survey);
		Configuration res = result.data;
		Assertions.assertEquals(200, result.status);
		// The result should have 4 traits
		Assertions.assertEquals(4, res.getTraits().size());
		Assertions.assertEquals(4, res.getData()[0][0].getValues().size());
		// Check the values for trait 3 and 4 are correct for germplasm 3
		Assertions.assertEquals("7", res.getData()[2][0].getValues().get(2));
		Assertions.assertEquals("2022-02-02", res.getData()[2][0].getDates().get(2));
		Assertions.assertEquals("3.5", res.getData()[2][0].getValues().get(3));
		Assertions.assertEquals("2022-02-01", res.getData()[2][0].getDates().get(3));
	}

	/**
	 * Test adding Germplasm to surveys. First, add two new Germplasm to the survey, then compare the result,
	 * then replace one of them with another new one and compare result.
	 * The server should be able to join them accordingly.
	 */
	@Order(7)
	@Test
	void testAddGermplasm()
	{
		Cell[][] data = survey.getData();

		// Copy data across
		Cell[][] newData = new Cell[data.length + 2][data[0].length];
		System.arraycopy(data, 0, newData, 0, data.length);

		// Add two new Germplasm
		newData[data.length] = new Cell[]{new Cell().setName("g4").setValues(Arrays.asList("yellow", null, "1", "2")).setDates(Arrays.asList("2022-03-01", null, "2022-03-02", "2022-03-03"))};
		newData[data.length + 1] = new Cell[]{new Cell().setName("g5").setValues(Arrays.asList("white", null, "3", "4")).setDates(Arrays.asList("2022-03-04", null, "2022-03-05", "2022-03-06"))};
		survey.setData(newData);

		// Send to server, compare response to check data is there.
		ApiResult<Configuration> result = sendConfiguration(survey);
		Assertions.assertEquals(200, result.status);
		Configuration res = result.data;
		Assertions.assertEquals(newData.length, res.getData().length);
		Assertions.assertEquals("g4", res.getData()[data.length][0].getName());
		Assertions.assertEquals("yellow", res.getData()[data.length][0].getValues().get(0));
		Assertions.assertEquals("g5", res.getData()[data.length + 1][0].getName());
		Assertions.assertEquals("white", res.getData()[data.length + 1][0].getValues().get(0));

		// Now let's change some data and also add another germplasm
		newData[data.length][0].getValues().set(0, "white");
		newData[data.length][0].getDates().set(0, "2022-03-02");
		newData[data.length + 1] = new Cell[]{new Cell().setName("g6").setValues(Arrays.asList("yellow", null, "5", null)).setDates(Arrays.asList("2022-03-07", null, "2022-03-08", null))};
		survey.setData(newData);

		// Send again, then check whether it has been joined correctly
		result = sendConfiguration(survey);
		Assertions.assertEquals(200, result.status);
		res = result.data;
		Assertions.assertEquals(newData.length + 1, res.getData().length);
		Assertions.assertEquals("g4", res.getData()[data.length][0].getName());
		Assertions.assertEquals("white", res.getData()[data.length][0].getValues().get(0));
		Assertions.assertEquals("2022-03-02", res.getData()[data.length][0].getDates().get(0));
		Assertions.assertEquals("g5", res.getData()[data.length + 1][0].getName());
		Assertions.assertEquals("white", res.getData()[data.length + 1][0].getValues().get(0));
		Assertions.assertEquals("g6", res.getData()[data.length + 2][0].getName());
		Assertions.assertEquals("yellow", res.getData()[data.length + 2][0].getValues().get(0));

		survey = res;

		// Now lets just send the same config back and expect it to come back without changes
		result = sendConfiguration(res);
		Assertions.assertEquals(200, result.status);
		assertConfigEquals(survey, result.data);

		survey = result.data;
	}

	/**
	 * Test the GET call to make sure it returns the same (correct) data.
	 */
	@Order(8)
	@RepeatedTest(3)
	void testGetConfig()
	{
		ApiResult<Configuration> result = getConfiguration(survey.getUuid());
		Assertions.assertEquals(200, result.status);
		assertConfigEquals(survey, result.data);

		// Method not allowed for GET request without parameter
		result = getConfiguration(null);
		Assertions.assertEquals(405, result.status);

		// Not found for GET request with wrong parameter
		result = getConfiguration("something-something");
		Assertions.assertEquals(404, result.status);
	}

	@Order(9)
	@Test
	void testStructureChangeError()
	{
		survey.setRows(40);
		survey.setCols(2);
		ApiResult<Configuration> result = sendConfiguration(survey);
		Assertions.assertEquals(409, result.status);
		survey.setRows(survey.getData().length);
		survey.setCols(survey.getData()[0].length);
	}

	@Order(10)
	@Test
	void testCheckForUpdate()
	{
		// Check whether there's an update if you set the timestamp to something very long ago
		ApiResult<List<Boolean>> result = checkForUpdate(survey, new Timestamp(survey.getLastUpdatedOn().getTime() - 1000));
		Assertions.assertEquals(200, result.status);
		List<Boolean> data = result.data;
		Assertions.assertEquals(1, data.size());
		Assertions.assertEquals(true, data.get(0));

		// Send it again twice and assume that the response is always the same
		ApiResult<Configuration> configResult = sendConfiguration(survey);
		Assertions.assertEquals(200, configResult.status);
		assertConfigEquals(survey, configResult.data);
		Configuration temp = configResult.data;
		configResult = sendConfiguration(temp);
		Assertions.assertEquals(200, configResult.status);
		assertConfigEquals(temp, configResult.data);

		result = checkForUpdate(survey, survey.getLastUpdatedOn());
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
								 .path(survey.getUuid())
								 .queryParam("name", survey.getName());

		Response resp = target.request(MediaType.APPLICATION_JSON)
							  .delete();

		Assertions.assertEquals(200, resp.getStatus());
	}
}
