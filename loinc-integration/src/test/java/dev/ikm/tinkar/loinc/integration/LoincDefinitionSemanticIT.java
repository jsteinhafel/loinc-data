package dev.ikm.tinkar.loinc.integration;

import dev.ikm.maven.LoincUtility;
import dev.ikm.tinkar.common.id.PublicId;
import dev.ikm.tinkar.common.id.PublicIds;
import dev.ikm.tinkar.common.util.uuid.UuidT5Generator;
import dev.ikm.tinkar.common.util.uuid.UuidUtil;
import dev.ikm.tinkar.component.Component;
import dev.ikm.tinkar.coordinate.Calculators;
import dev.ikm.tinkar.coordinate.Coordinates;
import dev.ikm.tinkar.coordinate.stamp.StampCoordinateRecord;
import dev.ikm.tinkar.coordinate.stamp.StateSet;
import dev.ikm.tinkar.coordinate.stamp.calculator.Latest;
import dev.ikm.tinkar.coordinate.stamp.calculator.StampCalculator;
import dev.ikm.tinkar.coordinate.stamp.calculator.StampCalculatorWithCache;
import dev.ikm.tinkar.entity.ConceptRecord;
import dev.ikm.tinkar.entity.ConceptVersionRecord;
import dev.ikm.tinkar.entity.EntityService;
import dev.ikm.tinkar.entity.PatternEntityVersion;
import dev.ikm.tinkar.entity.SemanticRecord;
import dev.ikm.tinkar.entity.SemanticVersionRecord;
import dev.ikm.tinkar.terms.EntityProxy;
import dev.ikm.tinkar.terms.TinkarTerm;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static dev.ikm.tinkar.terms.TinkarTerm.DEFINITION_DESCRIPTION_TYPE;
import static dev.ikm.tinkar.terms.TinkarTerm.DESCRIPTION_NOT_CASE_SENSITIVE;
import static dev.ikm.tinkar.terms.TinkarTerm.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE;
import static dev.ikm.tinkar.terms.TinkarTerm.REGULAR_NAME_DESCRIPTION_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LoincDefinitionSemanticIT extends LoincAbstractIntegrationTest {

	public static final String MODULE_DESCRIPTION = "LOINC® modules (SOLOR)";
	public static final String LOINC_NUMBER = "LOINC Number";

	public static final String AUTHOR_CONCEPT = "Regenstrief Institute, Inc. Author";
	public static final String OWL_EXPRESSION_ELEMENT = "ObjectIntersectionOf";

	/**
	 * Test LoincRowConcepts Loinc.csv Semantics.
	 *
	 * @result Reads content from file and validates Concept of Semantics by calling
	 *         private method assertConcept().
	 */
	@Test
	public void testLoincDefinitionSemantics() throws IOException {
		String sourceFilePath = "../loinc-origin/target/origin-sources";
		String errorFile = "target/failsafe-reports/LoincCsv_not_found.txt";

		String absolutePath = findFilePath(sourceFilePath, "Loinc.csv");
		int notFound = processLoincFile(absolutePath, errorFile);

		assertEquals(0, notFound,
				"Unable to find " + notFound + " Loinc.csv semantics. Details written to " + errorFile);
	}

	@Override
	protected boolean assertLine(String[] columns) {
		UUID id = uuid(columns[0]);

		String longCommonName = removeQuotes(columns[25]);
		String consumerName = removeQuotes(columns[12]);
		String shortName = removeQuotes(columns[20]);
		String relatedNames2 = removeQuotes(columns[19]);
		String displayName = removeQuotes(columns[39]);
		String definitionDescription = removeQuotes(columns[10]);

		/*
		 * TinkarTerm.DEFINITION_DESCRIPTION_TYPE;
		 * TinkarTerm.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE;
		 * TinkarTerm.REGULAR_NAME_DESCRIPTION_TYPE;
		 */

		System.out.println(">>>> ID: " + columns[0]);
		EntityProxy.Concept concept;
		concept = EntityProxy.Concept.make(PublicIds.of(id));

		EntityProxy.Concept descType = null;
		String term = "";

		// Create description semantics for non-empty fields
		if (!longCommonName.isEmpty()) {
			descType = FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE;
			term = longCommonName;
		}

		if (!consumerName.isEmpty()) {
			descType = REGULAR_NAME_DESCRIPTION_TYPE;
			term = consumerName;
		}

		if (!shortName.isEmpty()) {
			descType = REGULAR_NAME_DESCRIPTION_TYPE;
			term = shortName;
		}

		if (!relatedNames2.isEmpty()) {
			descType = REGULAR_NAME_DESCRIPTION_TYPE;
			term = relatedNames2;
		}

		if (!displayName.isEmpty()) {
			descType = REGULAR_NAME_DESCRIPTION_TYPE;
			term = displayName;
		}

		if (!definitionDescription.isEmpty()) {
			descType = DEFINITION_DESCRIPTION_TYPE;
			term = definitionDescription;
		}

		EntityProxy.Concept caseSensitivityConcept = DESCRIPTION_NOT_CASE_SENSITIVE;

		StateSet active = null;
		if (columns[11].equals("ACTIVE") || columns[11].equals("TRIAL") || columns[11].equals("DISCOURAGED")) {
			active = StateSet.ACTIVE;
		} else {
			active = StateSet.INACTIVE;
		}

		StampCalculator stampCalc = StampCalculatorWithCache
				.getCalculator(StampCoordinateRecord.make(active, Coordinates.Position.LatestOnMaster()));

		System.out.println(" >>>> TEST CONCEPT >>> " + concept.toString() + term);
		SemanticRecord entity = EntityService.get().getEntityFast(uuid(concept.toString() + term));

		PatternEntityVersion latestDescriptionPattern = (PatternEntityVersion) Calculators.Stamp.DevelopmentLatest()
				.latest(TinkarTerm.DESCRIPTION_PATTERN).get();
		
		Latest<SemanticVersionRecord> latest = stampCalc.latest(entity);
		
		if (latest.isPresent()) {
			Component descriptionType = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.DESCRIPTION_TYPE,
					latest.get());
			
			Component caseSensitivity = latestDescriptionPattern
					.getFieldWithMeaning(TinkarTerm.DESCRIPTION_CASE_SIGNIFICANCE, latest.get());
			
			String text = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.TEXT_FOR_DESCRIPTION, latest.get());
			
			if (PublicId.equals(descriptionType.publicId(), TinkarTerm.DEFINITION_DESCRIPTION_TYPE)) {
				return descriptionType.equals(descType) && caseSensitivity.equals(caseSensitivityConcept)
						&& text.equals(term);
			}
		}

		return false;
	}

	private String removeQuotes(String column) {
		return column.replaceAll("^\"|\"$", "").trim();
	}
}
