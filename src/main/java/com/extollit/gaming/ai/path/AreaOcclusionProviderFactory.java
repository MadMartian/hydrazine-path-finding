package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.AreaOcclusionProvider;
import com.extollit.gaming.ai.path.model.IColumnarSpace;
import com.extollit.gaming.ai.path.model.IInstanceSpace;
import com.extollit.gaming.ai.path.model.IOcclusionProviderFactory;

class AreaOcclusionProviderFactory implements IOcclusionProviderFactory {
    static final AreaOcclusionProviderFactory INSTANCE = new AreaOcclusionProviderFactory();

    private AreaOcclusionProviderFactory() {}

    @Override
    public AreaOcclusionProvider fromInstanceSpace(IInstanceSpace instance, int cx0, int cz0, int cxN, int czN) {
        IColumnarSpace[][] array = new IColumnarSpace[czN - cz0 + 1][cxN - cx0 + 1];

        for (int cz = cz0; cz <= czN; ++cz)
            for (int cx = cx0; cx <= cxN; ++cx) {
                final IColumnarSpace columnarSpace = instance.columnarSpaceAt(cx, cz);
                if (columnarSpace != null)
                    array[cz - cz0][cx - cx0] = columnarSpace;
            }

        return new AreaOcclusionProvider(array, cx0, cz0);
    }
}
