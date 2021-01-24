/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package external.org.apache.commons.lang3.builder;

/**
 * <p>
 * The Builder interface is designed to designate a class as a <em>builder</em> 
 * object in the Builder design pattern. Builders are capable of creating and 
 * configuring objects or results that normally take multiple steps to construct 
 * or are very complex to derive. 
 * </p>
 * 
 * <p>
 * The builder interface defines a single method, {@link #build()}, that 
 * classes must implement. The result of this method should be the final 
 * configured object or result after all building operations are performed.
 * </p>
 * 
 * <p>
 * It is a recommended practice that the methods supplied to configure the 
 * object or result being built return a reference to {@code this} so that
 * method calls can be chained together.
 * </p>
 * 
 * <p>
 * Example Builder:
 * <code><pre>
 * class FontBuilder implements Builder&lt;Font&gt; {
 *     private Font font;
 *     
 *     public FontBuilder(String fontName) {
 *         this.font = new Font(fontName, Font.PLAIN, 12);
 *     }
 * 
 *     public FontBuilder bold() {
 *         this.font = this.font.deriveFont(Font.BOLD);
 *         return this; // Reference returned so calls can be chained
 *     }
 *     
 *     public FontBuilder size(float pointSize) {
 *         this.font = this.font.deriveFont(pointSize);
 *         return this; // Reference returned so calls can be chained
 *     }
 * 
 *     // Other Font construction methods
 * 
 *     public Font build() {
 *         return this.font;
 *     }
 * }
 * </pre></code>
 * 
 * Example Builder Usage:
 * <code><pre>
 * Font bold14ptSansSerifFont = new FontBuilder(Font.SANS_SERIF).bold()
 *                                                              .size(14.0f)
 *                                                              .build();
 * </pre></code>
 * </p>
 * 
 * @param <T> the type of object that the builder will construct or compute.
 * 
 * @since 3.0
 * @version $Id: Builder.java 1088899 2011-04-05 05:31:27Z bayard $
 */
public interface Builder<T> {

    /**
     * Returns a reference to the object being constructed or result being 
     * calculated by the builder.
     * 
     * @return the object constructed or result calculated by the builder.
     */
    public T build();
}
