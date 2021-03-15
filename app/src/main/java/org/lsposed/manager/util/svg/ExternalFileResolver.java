/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2021 LSPosed Contributors
 */

package org.lsposed.manager.util.svg;

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
