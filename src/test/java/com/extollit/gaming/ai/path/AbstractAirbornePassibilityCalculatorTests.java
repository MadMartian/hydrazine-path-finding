package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.FlagSampler;
import org.junit.Before;

abstract class AbstractAirbornePassibilityCalculatorTests extends AbstractHydrazinePathFinderTests {
    protected AirbornePassibilityCalculator calculator;
    protected FlagSampler flagSampler;

    @Before
    public void setup() {
        super.setup();

        this.flagSampler = new FlagSampler(super.occlusionProvider);

        this.calculator = new AirbornePassibilityCalculator(super.instanceSpace);
        this.calculator.applySubject(super.pathingEntity);
    }
}
