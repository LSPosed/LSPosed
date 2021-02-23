package io.github.lsposed.manager.util.svg;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.util.Base64;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGExternalFileResolver;

public class ExternalFileResolver extends SVGExternalFileResolver {
    @Override
    public Bitmap resolveImage(String filename) {
        if (filename.startsWith("data:image/svg+xml;base64,")) {
            // com.shatyuka.zhiliao
            try {
                String base64 = filename.substring(filename.indexOf(","));
                SVG svg = SVG.getFromString(new String(Base64.decode(base64, Base64.DEFAULT)));
                float width = svg.getDocumentWidth();
                float height = svg.getDocumentHeight();
                Bitmap bitmap;
                if (width > 0 && height > 0) {
                    bitmap = Bitmap.createBitmap(Math.round(width), Math.round(width), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    svg.renderToCanvas(canvas);
                } else {
                    Picture picture = svg.renderToPicture();
                    bitmap = Bitmap.createBitmap(picture.getWidth(), picture.getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    canvas.drawPicture(picture);
                }
                return bitmap;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return super.resolveImage(filename);
    }
}
