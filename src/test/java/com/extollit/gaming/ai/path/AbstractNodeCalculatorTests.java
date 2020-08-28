package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.FlagSampler;
import com.extollit.gaming.ai.path.model.IInstanceSpace;
import com.extollit.gaming.ai.path.model.INodeCalculator;
import org.junit.Before;

public abstract class AbstractNodeCalculatorTests extends AbstractHydrazinePathFinderTests {
    protected INodeCalculator calculator;
    protected FlagSampler flagSampler;

    @Before
    public void setup() {
        super.setup();

        this.flagSampler = new FlagSampler(super.occlusionProvider);

        this.calculator = createCalculator(super.instanceSpace);
        this.calculator.applySubject(super.pathingEntity);
    }

    protected abstract INodeCalculator createCalculator(IInstanceSpace instanceSpace);
}
