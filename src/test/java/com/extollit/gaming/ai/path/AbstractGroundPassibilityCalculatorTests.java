package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.FlagSampler;
import org.junit.Before;

abstract class AbstractGroundPassibilityCalculatorTests extends AbstractHydrazinePathFinderTests {
    protected GroundPassibilityCalculator calculator;
    protected FlagSampler flagSampler;

    @Before
    public void setup() {
        super.setup();

        this.flagSampler = new FlagSampler(super.occlusionProvider);

        this.calculator = new GroundPassibilityCalculator(super.instanceSpace);
        this.calculator.applySubject(super.pathingEntity);
    }
}
