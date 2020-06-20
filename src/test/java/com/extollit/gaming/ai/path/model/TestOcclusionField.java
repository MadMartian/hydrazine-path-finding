package com.extollit.gaming.ai.path.model;

public class TestOcclusionField extends OcclusionField {
    public final AreaInit area;

    public TestOcclusionField() {
        this(null);
    }
    public TestOcclusionField(AreaInit area) {
        this.area = area;
    }

    @Override
    public String toString() {
        return this.area == null ? "center" : this.area.name();
    }
}
