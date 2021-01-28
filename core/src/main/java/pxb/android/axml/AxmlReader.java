/*
 * Copyright (c) 2009-2013 Panxiaobo
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pxb.android.axml;

import static pxb.android.axml.AxmlParser.END_FILE;
import static pxb.android.axml.AxmlParser.END_NS;
import static pxb.android.axml.AxmlParser.END_TAG;
import static pxb.android.axml.AxmlParser.START_FILE;
import static pxb.android.axml.AxmlParser.START_NS;
import static pxb.android.axml.AxmlParser.START_TAG;
import static pxb.android.axml.AxmlParser.TEXT;

import java.io.IOException;
import java.util.Stack;

import de.robv.android.xposed.XposedBridge;

/**
 * a class to read android axml
 * 
 * @author <a href="mailto:pxb1988@gmail.com">Panxiaobo</a>
 */
public class AxmlReader {
	public static final NodeVisitor EMPTY_VISITOR = new NodeVisitor() {

		@Override
		public NodeVisitor child(String ns, String name) {
			return this;
		}

	};
	final AxmlParser parser;

	public AxmlReader(byte[] data) {
		super();
		this.parser = new AxmlParser(data);
	}

	public void accept(final AxmlVisitor av) throws IOException {
		Stack<NodeVisitor> nvs = new Stack<NodeVisitor>();
		NodeVisitor tos = av;
		while (true) {
			int type = parser.next();
			switch (type) {
			case START_FILE:
				break;
			case START_TAG:
				nvs.push(tos);
				tos = tos.child(parser.getNamespaceUri(), parser.getName());
				if (tos != null) {
					if (tos != EMPTY_VISITOR) {
						tos.line(parser.getLineNumber());
						for (int i = 0; i < parser.getAttrCount(); i++) {
							tos.attr(parser.getAttrNs(i), parser.getAttrName(i), parser.getAttrResId(i),
									parser.getAttrType(i), parser.getAttrValue(i));
						}
					}
				} else {
					tos = EMPTY_VISITOR;
				}
				break;
			case END_TAG:
				tos.end();
				tos = nvs.pop();
				break;
			case START_NS:
				av.ns(parser.getNamespacePrefix(), parser.getNamespaceUri(), parser.getLineNumber());
				break;
			case END_NS:
				break;
			case TEXT:
				tos.text(parser.getLineNumber(), parser.getText());
				break;
			case END_FILE:
				return;
			default:
				XposedBridge.log("Unsupported tag: " + type);
			}
		}
	}
}
