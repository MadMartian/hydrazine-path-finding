package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.FlagSampler;
import com.extollit.gaming.ai.path.model.IInstanceSpace;
import com.extollit.gaming.ai.path.model.IPointPassibilityCalculator;
import org.junit.Before;

public abstract class AbstractPassibilityCalculatorTests extends AbstractHydrazinePathFinderTests {
    protected IPointPassibilityCalculator calculator;
    protected FlagSampler flagSampler;

    @Before
    public void setup() {
        super.setup();

        this.flagSampler = new FlagSampler(super.occlusionProvider);

        this.calculator = createCalculator(super.instanceSpace);
        this.calculator.applySubject(super.pathingEntity);
    }

    protected abstract IPointPassibilityCalculator createCalculator(IInstanceSpace instanceSpace);
}
