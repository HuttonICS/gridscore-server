import com.google.gson.Gson;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.*;
import jhi.gridscore.server.pojo.Configuration;
import jhi.gridscore.server.pojo.*;
import org.junit.jupiter.api.*;

import java.util.Arrays;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SurveyMultiSharingTest extends ConfigTest
{
	private static Configuration survey;
	private Gson gson = new Gson();

	/**
	 * Create the initial configuration.
	 */
	@BeforeAll
	static void setUp()
		throws Exception
	{
		survey = new Configuration();
		survey.setName("Survey-test-multi");
		survey.setRows(3);
		survey.setCols(1);
		survey.setTraits(Arrays.asList(
			new Trait().setName("t1").setType("categorical").setmType("multi").setRestrictions(new Restrictions().setCategories(Arrays.asList("yellow", "white"))),
			new Trait().setName("t2").setType("date").setmType("multi")
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
		survey.setDatasetType(DatasetType.SURVEY);

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
		survey = result.data;
		Assertions.assertNotNull(survey.getUuid());
	}

	@Test
	@Order(2)
	void shareActualData()
	{
		Cell cell = survey.getData()[0][0];
		cell.getValues().set(0, gson.toJson(new String[]{"yellow", "white"}));
		cell.getDates().set(0, gson.toJson(new String[]{"2022-01-03", "2022-01-02"}));
		cell.getValues().set(1, gson.toJson(new String[]{"2022-03-03", "2022-03-04"}));
		cell.getDates().set(1, gson.toJson(new String[]{"2022-03-03", "2022-03-04"}));

		ApiResult<Configuration> result = sendConfiguration(survey);
		Assertions.assertEquals(200, result.status);
		Assertions.assertEquals(gson.toJson(new String[]{"white", "yellow"}), result.data.getData()[0][0].getValues().get(0));
		Assertions.assertEquals(gson.toJson(new String[]{"2022-01-02", "2022-01-03"}), result.data.getData()[0][0].getDates().get(0));

		survey = result.data;

		cell = survey.getData()[0][0];
		cell.getValues().set(0, gson.toJson(new String[]{"yellow"}));
		cell.getDates().set(0, gson.toJson(new String[]{"2022-01-01"}));

		result = sendConfiguration(survey);
		Assertions.assertEquals(200, result.status);
		Assertions.assertEquals(gson.toJson(new String[]{"yellow", "white", "yellow"}), result.data.getData()[0][0].getValues().get(0));
		Assertions.assertEquals(gson.toJson(new String[]{"2022-01-01", "2022-01-02", "2022-01-03"}), result.data.getData()[0][0].getDates().get(0));
	}

	/**
	 * Test adding Germplasm to surveys. First, add two new Germplasm to the survey, then compare the result.
	 * The server should be able to join them accordingly.
	 */
	@Order(3)
	@Test
	void testAddGermplasm()
	{
		Cell[][] data = survey.getData();

		// Copy data across
		Cell[][] newData = new Cell[data.length + 2][data[0].length];
		System.arraycopy(data, 0, newData, 0, data.length);

		// Add two new Germplasm
		newData[data.length] = new Cell[]{new Cell().setName("g4").setValues(Arrays.asList(gson.toJson(new String[]{"white", "yellow"}), gson.toJson(new String[]{"2022-04-04", "2022-04-05"}))).setDates(Arrays.asList(gson.toJson(new String[]{"2022-04-04", "2022-04-05"}), gson.toJson(new String[]{"2022-04-04", "2022-04-05"})))};
		newData[data.length + 1] = new Cell[]{new Cell().setName("g5").setValues(Arrays.asList(gson.toJson(new String[]{"yellow", "white"}), gson.toJson(new String[]{"2022-04-04", "2022-04-05"}))).setDates(Arrays.asList(gson.toJson(new String[]{"2022-04-04", "2022-04-05"}), gson.toJson(new String[]{"2022-04-04", "2022-04-05"})))};
		survey.setData(newData);

		// Send to server, compare response to check data is there.
		ApiResult<Configuration> result = sendConfiguration(survey);
		Assertions.assertEquals(200, result.status);
		Configuration res = result.data;
		Assertions.assertEquals(newData.length, res.getData().length);
		Assertions.assertEquals("g4", res.getData()[data.length][0].getName());
		Assertions.assertEquals(gson.toJson(new String[]{"white", "yellow"}), res.getData()[data.length][0].getValues().get(0));
		Assertions.assertEquals("g5", res.getData()[data.length + 1][0].getName());
		Assertions.assertEquals(gson.toJson(new String[]{"yellow", "white"}), res.getData()[data.length + 1][0].getValues().get(0));

		survey = res;

		// Now lets just send the same config back and expect it to come back without changes
		result = sendConfiguration(res);
		Assertions.assertEquals(200, result.status);
		assertConfigEquals(survey, result.data);

		survey = result.data;
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
