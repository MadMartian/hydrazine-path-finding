package com.extollit.gaming.ai.path.model;

public class TestColumnarOcclusionFieldList extends ColumnarOcclusionFieldList {
    public TestColumnarOcclusionFieldList(IColumnarSpace container) {
        super(container);
    }

    @Override
    protected OcclusionField createOcclusionField(int cx, int cy, int cz) {
        return new TestOcclusionField(OcclusionField.AreaInit.given(cx, cy, cz));
    }
}
