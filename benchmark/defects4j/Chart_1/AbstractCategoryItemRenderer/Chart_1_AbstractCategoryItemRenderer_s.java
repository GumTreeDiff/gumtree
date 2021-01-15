/* ===========================================================
 * JFreeChart : a free chart library for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2000-2010, by Object Refinery Limited and Contributors.
 *
 * Project Info:  http://www.jfree.org/jfreechart/index.html
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc.
 * in the United States and other countries.]
 *
 * ---------------------------------
 * AbstractCategoryItemRenderer.java
 * ---------------------------------
 * (C) Copyright 2002-2010, by Object Refinery Limited.
 *
 * Original Author:  David Gilbert (for Object Refinery Limited);
 * Contributor(s):   Richard Atkinson;
 *                   Peter Kolb (patch 2497611);
 *
 * Changes:
 * --------
 * 29-May-2002 : Version 1 (DG);
 * 06-Jun-2002 : Added accessor methods for the tool tip generator (DG);
 * 11-Jun-2002 : Made constructors protected (DG);
 * 26-Jun-2002 : Added axis to initialise method (DG);
 * 05-Aug-2002 : Added urlGenerator member variable plus accessors (RA);
 * 22-Aug-2002 : Added categoriesPaint attribute, based on code submitted by
 *               Janet Banks.  This can be used when there is only one series,
 *               and you want each category item to have a different color (DG);
 * 01-Oct-2002 : Fixed errors reported by Checkstyle (DG);
 * 29-Oct-2002 : Fixed bug where background image for plot was not being
 *               drawn (DG);
 * 05-Nov-2002 : Replaced references to CategoryDataset with TableDataset (DG);
 * 26-Nov 2002 : Replaced the isStacked() method with getRangeType() (DG);
 * 09-Jan-2003 : Renamed grid-line methods (DG);
 * 17-Jan-2003 : Moved plot classes into separate package (DG);
 * 25-Mar-2003 : Implemented Serializable (DG);
 * 12-May-2003 : Modified to take into account the plot orientation (DG);
 * 12-Aug-2003 : Very minor javadoc corrections (DB)
 * 13-Aug-2003 : Implemented Cloneable (DG);
 * 16-Sep-2003 : Changed ChartRenderingInfo --> PlotRenderingInfo (DG);
 * 05-Nov-2003 : Fixed marker rendering bug (833623) (DG);
 * 21-Jan-2004 : Update for renamed method in ValueAxis (DG);
 * 11-Feb-2004 : Modified labelling for markers (DG);
 * 12-Feb-2004 : Updated clone() method (DG);
 * 15-Apr-2004 : Created a new CategoryToolTipGenerator interface (DG);
 * 05-May-2004 : Fixed bug (948310) where interval markers extend outside axis
 *               range (DG);
 * 14-Jun-2004 : Fixed bug in drawRangeMarker() method - now uses 'paint' and
 *               'stroke' rather than 'outlinePaint' and 'outlineStroke' (DG);
 * 15-Jun-2004 : Interval markers can now use GradientPaint (DG);
 * 30-Sep-2004 : Moved drawRotatedString() from RefineryUtilities
 *               --> TextUtilities (DG);
 * 01-Oct-2004 : Fixed bug 1029697, problem with label alignment in
 *               drawRangeMarker() method (DG);
 * 07-Jan-2005 : Renamed getRangeExtent() --> findRangeBounds() (DG);
 * 21-Jan-2005 : Modified return type of calculateRangeMarkerTextAnchorPoint()
 *               method (DG);
 * 08-Mar-2005 : Fixed positioning of marker labels (DG);
 * 20-Apr-2005 : Added legend label, tooltip and URL generators (DG);
 * 01-Jun-2005 : Handle one dimension of the marker label adjustment
 *               automatically (DG);
 * 09-Jun-2005 : Added utility method for adding an item entity (DG);
 * ------------- JFREECHART 1.0.x ---------------------------------------------
 * 01-Mar-2006 : Updated getLegendItems() to check seriesVisibleInLegend
 *               flags (DG);
 * 20-Jul-2006 : Set dataset and series indices in LegendItem (DG);
 * 23-Oct-2006 : Draw outlines for interval markers (DG);
 * 24-Oct-2006 : Respect alpha setting in markers, as suggested by Sergei
 *               Ivanov in patch 1567843 (DG);
 * 30-Nov-2006 : Added a check for series visibility in the getLegendItem()
 *               method (DG);
 * 07-Dec-2006 : Fix for equals() method (DG);
 * 22-Feb-2007 : Added createState() method (DG);
 * 01-Mar-2007 : Fixed interval marker drawing (patch 1670686 thanks to
 *               Sergei Ivanov) (DG);
 * 20-Apr-2007 : Updated getLegendItem() for renderer change, and deprecated
 *               itemLabelGenerator, toolTipGenerator and itemURLGenerator
 *               override fields (DG);
 * 18-May-2007 : Set dataset and seriesKey for LegendItem (DG);
 * 20-Jun-2007 : Removed deprecated code and removed JCommon dependencies (DG);
 * 27-Jun-2007 : Added some new methods with 'notify' argument, renamed
 *               methods containing 'ItemURL' to just 'URL' (DG);
 * 06-Jul-2007 : Added annotation support (DG);
 * 17-Jun-2008 : Apply legend shape, font and paint attributes (DG);
 * 26-Jun-2008 : Added crosshair support (DG);
 * 25-Nov-2008 : Fixed bug in findRangeBounds() method (DG);
 * 14-Jan-2009 : Update initialise() to store visible series indices (PK);
 * 21-Jan-2009 : Added drawRangeLine() method (DG);
 * 28-Jan-2009 : Updated for changes to CategoryItemRenderer interface (DG);
 * 27-Mar-2009 : Added new findRangeBounds() method to account for hidden
 *               series (DG);
 * 01-Apr-2009 : Added new addEntity() method (DG);
 * 09-Feb-2010 : Fixed bug 2947660 (DG);
 *
 */

package org.jfree.chart.renderer.category;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.RenderingSource;
import org.jfree.chart.annotations.CategoryAnnotation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.labels.CategorySeriesLabelGenerator;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategorySeriesLabelGenerator;
import org.jfree.chart.plot.CategoryCrosshairState;
import org.jfree.chart.plot.CategoryMarker;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.text.TextUtilities;
import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.chart.util.GradientPaintTransformer;
import org.jfree.chart.util.Layer;
import org.jfree.chart.util.LengthAdjustmentType;
import org.jfree.chart.util.ObjectList;
import org.jfree.chart.util.ObjectUtilities;
import org.jfree.chart.util.PublicCloneable;
import org.jfree.chart.util.RectangleAnchor;
import org.jfree.chart.util.RectangleEdge;
import org.jfree.chart.util.RectangleInsets;
import org.jfree.chart.util.SortOrder;
import org.jfree.data.Range;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.CategoryDatasetSelectionState;
import org.jfree.data.category.SelectableCategoryDataset;
import org.jfree.data.general.DatasetUtilities;

/**
 * An abstract base class that you can use to implement a new
 * {@link CategoryItemRenderer}.  When you create a new
 * {@link CategoryItemRenderer} you are not required to extend this class,
 * but it makes the job easier.
 */
public abstract class AbstractCategoryItemRenderer extends AbstractRenderer
        implements CategoryItemRenderer, Cloneable, PublicCloneable,
        Serializable {

    /** For serialization. */
    private static final long serialVersionUID = 1247553218442497391L;

    /** The plot that the renderer is assigned to. */
    private CategoryPlot plot;

    /** A list of item label generators (one per series). */
    private ObjectList itemLabelGeneratorList;

    /** The base item label generator. */
    private CategoryItemLabelGenerator baseItemLabelGenerator;

    /** A list of tool tip generators (one per series). */
    private ObjectList toolTipGeneratorList;

    /** The base tool tip generator. */
    private CategoryToolTipGenerator baseToolTipGenerator;

    /** A list of label generators (one per series). */
    private ObjectList urlGeneratorList;

    /** The base label generator. */
    private CategoryURLGenerator baseURLGenerator;

    /** The legend item label generator. */
    private CategorySeriesLabelGenerator legendItemLabelGenerator;

    /** The legend item tool tip generator. */
    private CategorySeriesLabelGenerator legendItemToolTipGenerator;

    /** The legend item URL generator. */
    private CategorySeriesLabelGenerator legendItemURLGenerator;

    /**
     * Annotations to be drawn in the background layer ('underneath' the data
     * items).
     *
     * @since 1.2.0
     */
    private List backgroundAnnotations;

    /**
     * Annotations to be drawn in the foreground layer ('on top' of the data
     * items).
     *
     * @since 1.2.0
     */
    private List foregroundAnnotations;

    /** The number of rows in the dataset (temporary record). */
    private transient int rowCount;

    /** The number of columns in the dataset (temporary record). */
    private transient int columnCount;

    /**
     * Creates a new renderer with no tool tip generator and no URL generator.
     * The defaults (no tool tip or URL generators) have been chosen to
     * minimise the processing required to generate a default chart.  If you
     * require tool tips or URLs, then you can easily add the required
     * generators.
     */
    protected AbstractCategoryItemRenderer() {
        this.itemLabelGeneratorList = new ObjectList();
        this.toolTipGeneratorList = new ObjectList();
        this.urlGeneratorList = new ObjectList();
        this.legendItemLabelGenerator
                = new StandardCategorySeriesLabelGenerator();
        this.backgroundAnnotations = new ArrayList();
        this.foregroundAnnotations = new ArrayList();
    }

    /**
     * Returns the number of passes through the dataset required by the
     * renderer.  This method returns <code>1</code>, subclasses should
     * override if they need more passes.
     *
     * @return The pass count.
     */
    public int getPassCount() {
        return 1;
    }

    /**
     * Returns the plot that the renderer has been assigned to (where
     * <code>null</code> indicates that the renderer is not currently assigned
     * to a plot).
     *
     * @return The plot (possibly <code>null</code>).
     *
     * @see #setPlot(CategoryPlot)
     */
    public CategoryPlot getPlot() {
        return this.plot;
    }

    /**
     * Sets the plot that the renderer has been assigned to.  This method is
     * usually called by the {@link CategoryPlot}, in normal usage you
     * shouldn't need to call this method directly.
     *
     * @param plot  the plot (<code>null</code> not permitted).
     *
     * @see #getPlot()
     */
    public void setPlot(CategoryPlot plot) {
        if (plot == null) {
            throw new IllegalArgumentException("Null 'plot' argument.");
        }
        this.plot = plot;
    }

    // ITEM LABEL GENERATOR

    /**
     * Returns the item label generator for a data item.  This implementation
     * returns the series item label generator if one is defined, otherwise
     * it returns the default item label generator (which may be
     * <code>null</code>).
     *
     * @param row  the row index (zero based).
     * @param column  the column index (zero based).
     * @param selected  is the item selected?
     *
     * @return The generator (possibly <code>null</code>).
     *
     * @since 1.2.0
     */
    public CategoryItemLabelGenerator getItemLabelGenerator(int row,
            int column, boolean selected) {
        CategoryItemLabelGenerator generator = (CategoryItemLabelGenerator)
                this.itemLabelGeneratorList.get(row);
        if (generator == null) {
            generator = this.baseItemLabelGenerator;
        }
        return generator;
    }

    /**
     * Returns the item label generator for a series.
     *
     * @param series  the series index (zero based).
     *
     * @return The generator (possibly <code>null</code>).
     *
     * @see #setSeriesItemLabelGenerator(int, CategoryItemLabelGenerator)
     */
    public CategoryItemLabelGenerator getSeriesItemLabelGenerator(int series) {
        return (CategoryItemLabelGenerator) this.itemLabelGeneratorList.get(
                series);
    }

    /**
     * Sets the item label generator for a series and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     *
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     *
     * @see #getSeriesItemLabelGenerator(int)
     */
    public void setSeriesItemLabelGenerator(int series,
            CategoryItemLabelGenerator generator) {
        setSeriesItemLabelGenerator(series, generator, true);
    }

    /**
     * Sets the item label generator for a series and, if requested, sends a
     * {@link RendererChangeEvent} to all registered listeners.
     *
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     *
     * @since 1.2.0
     *
     * @see #getSeriesItemLabelGenerator(int)
     */
    public void setSeriesItemLabelGenerator(int series,
            CategoryItemLabelGenerator generator, boolean notify) {
        this.itemLabelGeneratorList.set(series, generator);
        if (notify) {
            notifyListeners(new RendererChangeEvent(this));
        }
    }

    /**
     * Returns the base item label generator.
     *
     * @return The generator (possibly <code>null</code>).
     *
     * @see #setBaseItemLabelGenerator(CategoryItemLabelGenerator)
     */
    public CategoryItemLabelGenerator getBaseItemLabelGenerator() {
        return this.baseItemLabelGenerator;
    }

    /**
     * Sets the base item label generator and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     *
     * @param generator  the generator (<code>null</code> permitted).
     *
     * @see #getBaseItemLabelGenerator()
     */
    public void setBaseItemLabelGenerator(
            CategoryItemLabelGenerator generator) {
        setBaseItemLabelGenerator(generator, true);
    }

    /**
     * Sets the base item label generator and, if requested, sends a
     * {@link RendererChangeEvent} to all registered listeners.
     *
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     *
     * @since 1.2.0
     *
     * @see #getBaseItemLabelGenerator()
     */
    public void setBaseItemLabelGenerator(
            CategoryItemLabelGenerator generator, boolean notify) {
        this.baseItemLabelGenerator = generator;
        if (notify) {
            notifyListeners(new RendererChangeEvent(this));
        }
    }

    // TOOL TIP GENERATOR

    /**
     * Returns the tool tip generator that should be used for the specified
     * item.  You can override this method if you want to return a different
     * generator per item.
     *
     * @param row  the row index (zero-based).
     * @param column  the column index (zero-based).
     * @param selected  is the item selected?
     *
     * @return The generator (possibly <code>null</code>).
     *
     * @since 1.2.0
     */
    public CategoryToolTipGenerator getToolTipGenerator(int row, int column,
            boolean selected) {

        CategoryToolTipGenerator result = null;
        result = getSeriesToolTipGenerator(row);
        if (result == null) {
            result = this.baseToolTipGenerator;
        }
        return result;
    }

    /**
     * Returns the tool tip generator for the specified series (a "layer 1"
     * generator).
     *
     * @param series  the series index (zero-based).
     *
     * @return The tool tip generator (possibly <code>null</code>).
     *
     * @see #setSeriesToolTipGenerator(int, CategoryToolTipGenerator)
     */
    public CategoryToolTipGenerator getSeriesToolTipGenerator(int series) {
        return (CategoryToolTipGenerator) this.toolTipGeneratorList.get(series);
    }

    /**
     * Sets the tool tip generator for a series and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     *
     * @param series  the series index (zero-based).
     * @param generator  the generator (<code>null</code> permitted).
     *
     * @see #getSeriesToolTipGenerator(int)
     */
    public void setSeriesToolTipGenerator(int series,
            CategoryToolTipGenerator generator) {
        setSeriesToolTipGenerator(series, generator, true);
    }

    /**
     * Sets the tool tip generator for a series and sends a
     * {@link org.jfree.chart.event.RendererChangeEvent} to all registered
     * listeners.
     *
     * @param series  the series index (zero-based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     *
     * @since 1.2.0
     *
     * @see #getSeriesToolTipGenerator(int)
     */
    public void setSeriesToolTipGenerator(int series,
            CategoryToolTipGenerator generator, boolean notify) {
        this.toolTipGeneratorList.set(series, generator);
        if (notify) {
            notifyListeners(new RendererChangeEvent(this));
        }
    }

    /**
     * Returns the base tool tip generator (the "layer 2" generator).
     *
     * @return The tool tip generator (possibly <code>null</code>).
     *
     * @see #setBaseToolTipGenerator(CategoryToolTipGenerator)
     */
    public CategoryToolTipGenerator getBaseToolTipGenerator() {
        return this.baseToolTipGenerator;
    }

    /**
     * Sets the base tool tip generator and sends a {@link RendererChangeEvent}
     * to all registered listeners.
     *
     * @param generator  the generator (<code>null</code> permitted).
     *
     * @see #getBaseToolTipGenerator()
     */
    public void setBaseToolTipGenerator(CategoryToolTipGenerator generator) {
        setBaseToolTipGenerator(generator, true);
    }

    /**
     * Sets the base tool tip generator and sends a {@link RendererChangeEvent}
     * to all registered listeners.
     *
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     *
     * @since 1.2.0
     *
     * @see #getBaseToolTipGenerator()
     */
    public void setBaseToolTipGenerator(CategoryToolTipGenerator generator,
            boolean notify) {
        this.baseToolTipGenerator = generator;
        if (notify) {
            notifyListeners(new RendererChangeEvent(this));
        }
    }

    // URL GENERATOR

    /**
     * Returns the URL generator for a data item.
     *
     * @param row  the row index (zero based).
     * @param column  the column index (zero based).
     * @param selected  is the item selected?
     *
     * @return The URL generator.
     *
     * @since 1.2.0
     */
    public CategoryURLGenerator getURLGenerator(int row, int column, boolean
            selected) {
        CategoryURLGenerator generator
                = (CategoryURLGenerator) this.urlGeneratorList.get(row);
        if (generator == null) {
            generator = this.baseURLGenerator;
        }
        return generator;
    }

    /**
     * Returns the URL generator for a series.
     *
     * @param series  the series index (zero based).
     *
     * @return The URL generator for the series.
     *
     * @see #setSeriesURLGenerator(int, CategoryURLGenerator)
     */
    public CategoryURLGenerator getSeriesURLGenerator(int series) {
        return (CategoryURLGenerator) this.urlGeneratorList.get(series);
    }

    /**
     * Sets the URL generator for a series and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     *
     * @param series  the series index (zero based).
     * @param generator  the generator.
     *
     * @see #getSeriesURLGenerator(int)
     */
    public void setSeriesURLGenerator(int series,
            CategoryURLGenerator generator) {
        setSeriesURLGenerator(series, generator, true);
    }

    /**
     * Sets the URL generator for a series and, if requested, sends a
     * {@link RendererChangeEvent} to all registered listeners.
     *
     * @param series  the series index (zero based).
     * @param generator  the generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     *
     * @since 1.2.0
     *
     * @see #getSeriesURLGenerator(int)
     */
    public void setSeriesURLGenerator(int series,
            CategoryURLGenerator generator, boolean notify) {
        this.urlGeneratorList.set(series, generator);
        if (notify) {
            notifyListeners(new RendererChangeEvent(this));
        }
    }

    /**
     * Returns the base item URL generator.
     *
     * @return The item URL generator.
     *
     * @see #setBaseURLGenerator(CategoryURLGenerator)
     */
    public CategoryURLGenerator getBaseURLGenerator() {
        return this.baseURLGenerator;
    }

    /**
     * Sets the base item URL generator.
     *
     * @param generator  the item URL generator.
     *
     * @see #getBaseURLGenerator()
     */
    public void setBaseURLGenerator(CategoryURLGenerator generator) {
        setBaseURLGenerator(generator, true);
    }

    /**
     * Sets the base item URL generator.
     *
     * @param generator  the item URL generator (<code>null</code> permitted).
     * @param notify  notify listeners?
     *
     * @see #getBaseURLGenerator()
     *
     * @since 1.2.0
     */
    public void setBaseURLGenerator(CategoryURLGenerator generator,
            boolean notify) {
        this.baseURLGenerator = generator;
        if (notify) {
            notifyListeners(new RendererChangeEvent(this));
        }
    }

    // ANNOTATIONS

    /**
     * Adds an annotation and sends a {@link RendererChangeEvent} to all
     * registered listeners.  The annotation is added to the foreground
     * layer.
     *
     * @param annotation  the annotation (<code>null</code> not permitted).
     *
     * @since 1.2.0
     */
    public void addAnnotation(CategoryAnnotation annotation) {
        // defer argument checking
        addAnnotation(annotation, Layer.FOREGROUND);
    }

    /**
     * Adds an annotation to the specified layer.
     *
     * @param annotation  the annotation (<code>null</code> not permitted).
     * @param layer  the layer (<code>null</code> not permitted).
     *
     * @since 1.2.0
     */
    public void addAnnotation(CategoryAnnotation annotation, Layer layer) {
        if (annotation == null) {
            throw new IllegalArgumentException("Null 'annotation' argument.");
        }
        if (layer.equals(Layer.FOREGROUND)) {
            this.foregroundAnnotations.add(annotation);
            notifyListeners(new RendererChangeEvent(this));
        }
        else if (layer.equals(Layer.BACKGROUND)) {
            this.backgroundAnnotations.add(annotation);
            notifyListeners(new RendererChangeEvent(this));
        }
        else {
            // should never get here
            throw new RuntimeException("Unknown layer.");
        }
    }
    /**
     * Removes the specified annotation and sends a {@link RendererChangeEvent}
     * to all registered listeners.
     *
     * @param annotation  the annotation to remove (<code>null</code> not
     *                    permitted).
     *
     * @return A boolean to indicate whether or not the annotation was
     *         successfully removed.
     *
     * @since 1.2.0
     */
    public boolean removeAnnotation(CategoryAnnotation annotation) {
        boolean removed = this.foregroundAnnotations.remove(annotation);
        removed = removed & this.backgroundAnnotations.remove(annotation);
        notifyListeners(new RendererChangeEvent(this));
        return removed;
    }

    /**
     * Removes all annotations and sends a {@link RendererChangeEvent}
     * to all registered listeners.
     *
     * @since 1.2.0
     */
    public void removeAnnotations() {
        this.foregroundAnnotations.clear();
        this.backgroundAnnotations.clear();
        notifyListeners(new RendererChangeEvent(this));
    }

    /**
     * Returns the legend item label generator.
     *
     * @return The label generator (never <code>null</code>).
     *
     * @see #setLegendItemLabelGenerator(CategorySeriesLabelGenerator)
     */
    public CategorySeriesLabelGenerator getLegendItemLabelGenerator() {
        return this.legendItemLabelGenerator;
    }

    /**
     * Sets the legend item label generator and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     *
     * @param generator  the generator (<code>null</code> not permitted).
     *
     * @see #getLegendItemLabelGenerator()
     */
    public void setLegendItemLabelGenerator(
            CategorySeriesLabelGenerator generator) {
        if (generator == null) {
            throw new IllegalArgumentException("Null 'generator' argument.");
        }
        this.legendItemLabelGenerator = generator;
        fireChangeEvent();
    }

    /**
     * Returns the legend item tool tip generator.
     *
     * @return The tool tip generator (possibly <code>null</code>).
     *
     * @see #setLegendItemToolTipGenerator(CategorySeriesLabelGenerator)
     */
    public CategorySeriesLabelGenerator getLegendItemToolTipGenerator() {
        return this.legendItemToolTipGenerator;
    }

    /**
     * Sets the legend item tool tip generator and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     *
     * @param generator  the generator (<code>null</code> permitted).
     *
     * @see #setLegendItemToolTipGenerator(CategorySeriesLabelGenerator)
     */
    public void setLegendItemToolTipGenerator(
            CategorySeriesLabelGenerator generator) {
        this.legendItemToolTipGenerator = generator;
        fireChangeEvent();
    }

    /**
     * Returns the legend item URL generator.
     *
     * @return The URL generator (possibly <code>null</code>).
     *
     * @see #setLegendItemURLGenerator(CategorySeriesLabelGenerator)
     */
    public CategorySeriesLabelGenerator getLegendItemURLGenerator() {
        return this.legendItemURLGenerator;
    }

    /**
     * Sets the legend item URL generator and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     *
     * @param generator  the generator (<code>null</code> permitted).
     *
     * @see #getLegendItemURLGenerator()
     */
    public void setLegendItemURLGenerator(
            CategorySeriesLabelGenerator generator) {
        this.legendItemURLGenerator = generator;
        fireChangeEvent();
    }

    /**
     * Returns the number of rows in the dataset.  This value is updated in the
     * {@link AbstractCategoryItemRenderer#initialise} method.
     *
     * @return The row count.
     */
    public int getRowCount() {
        return this.rowCount;
    }

    /**
     * Returns the number of columns in the dataset.  This value is updated in
     * the {@link AbstractCategoryItemRenderer#initialise} method.
     *
     * @return The column count.
     */
    public int getColumnCount() {
        return this.columnCount;
    }

    /**
     * Creates a new state instance---this method is called from the
     * {@link #initialise(Graphics2D, Rectangle2D, CategoryPlot, int,
     * PlotRenderingInfo)} method.  Subclasses can override this method if
     * they need to use a subclass of {@link CategoryItemRendererState}.
     *
     * @param info  collects plot rendering info (<code>null</code> permitted).
     *
     * @return The new state instance (never <code>null</code>).
     *
     * @since 1.0.5
     */
    protected CategoryItemRendererState createState(PlotRenderingInfo info) {
        CategoryItemRendererState state = new CategoryItemRendererState(info);
        int[] visibleSeriesTemp = new int[this.rowCount];
        int visibleSeriesCount = 0;
        for (int row = 0; row < this.rowCount; row++) {
            if (isSeriesVisible(row)) {
                visibleSeriesTemp[visibleSeriesCount] = row;
                visibleSeriesCount++;
            }
        }
        int[] visibleSeries = new int[visibleSeriesCount];
        System.arraycopy(visibleSeriesTemp, 0, visibleSeries, 0,
                visibleSeriesCount);
        state.setVisibleSeriesArray(visibleSeries);
        return state;
    }

    /**
     * Initialises the renderer and returns a state object that will be used
     * for the remainder of the drawing process for a single chart.  The state
     * object allows for the fact that the renderer may be used simultaneously
     * by multiple threads (each thread will work with a separate state object).
     *
     * @param g2  the graphics device.
     * @param dataArea  the data area.
     * @param plot  the plot.
     * @param info  an object for returning information about the structure of
     *              the plot (<code>null</code> permitted).
     *
     * @return The renderer state.
     */
    public CategoryItemRendererState initialise(Graphics2D g2,
            Rectangle2D dataArea, CategoryPlot plot, CategoryDataset dataset,
            PlotRenderingInfo info) {

        setPlot(plot);
        if (dataset != null) {
            this.rowCount = dataset.getRowCount();
            this.columnCount = dataset.getColumnCount();
        }
        else {
            this.rowCount = 0;
            this.columnCount = 0;
        }
        CategoryItemRendererState state = createState(info);

        // determine if there is any selection state for the dataset
        CategoryDatasetSelectionState selectionState = null;
        if (dataset instanceof SelectableCategoryDataset) {
            SelectableCategoryDataset scd = (SelectableCategoryDataset) dataset;
            selectionState = scd.getSelectionState();
        }
        // if the selection state is still null, go to the selection source
        // and ask if it has state...
        if (selectionState == null && info != null) {
            ChartRenderingInfo cri = info.getOwner();
            if (cri != null) {
                RenderingSource rs = cri.getRenderingSource();
                selectionState = (CategoryDatasetSelectionState)
                        rs.getSelectionState(dataset);
            }
        }
        state.setSelectionState(selectionState);

        return state;
    }

    /**
     * Returns the range of values the renderer requires to display all the
     * items from the specified dataset.
     *
     * @param dataset  the dataset (<code>null</code> permitted).
     *
     * @return The range (or <code>null</code> if the dataset is
     *         <code>null</code> or empty).
     */
    public Range findRangeBounds(CategoryDataset dataset) {
        return findRangeBounds(dataset, false);
    }

    /**
     * Returns the range of values the renderer requires to display all the
     * items from the specified dataset.
     *
     * @param dataset  the dataset (<code>null</code> permitted).
     * @param includeInterval  include the y-interval if the dataset has one.
     *
     * @return The range (<code>null</code> if the dataset is <code>null</code>
     *         or empty).
     *
     * @since 1.0.13
     */
    protected Range findRangeBounds(CategoryDataset dataset,
            boolean includeInterval) {
        if (dataset == null) {
            return null;
        }
        if (getDataBoundsIncludesVisibleSeriesOnly()) {
            List visibleSeriesKeys = new ArrayList();
            int seriesCount = dataset.getRowCount();
            for (int s = 0; s < seriesCount; s++) {
                if (isSeriesVisible(s)) {
                    visibleSeriesKeys.add(dataset.getRowKey(s));
                }
            }
            return DatasetUtilities.findRangeBounds(dataset,
                    visibleSeriesKeys, includeInterval);
        }
        else {
            return DatasetUtilities.findRangeBounds(dataset, includeInterval);
        }
    }

    /**
     * Returns the Java2D coordinate for the middle of the specified data item.
     *
     * @param rowKey  the row key.
     * @param columnKey  the column key.
     * @param dataset  the dataset.
     * @param axis  the axis.
     * @param area  the data area.
     * @param edge  the edge along which the axis lies.
     *
     * @return The Java2D coordinate for the middle of the item.
     *
     * @since 1.0.11
     */
    public double getItemMiddle(Comparable rowKey, Comparable columnKey,
            CategoryDataset dataset, CategoryAxis axis, Rectangle2D area,
            RectangleEdge edge) {
        return axis.getCategoryMiddle(columnKey, dataset.getColumnKeys(), area,
                edge);
    }

    /**
     * Draws a background for the data area.  The default implementation just
     * gets the plot to draw the background, but some renderers will override
     * this behaviour.
     *
     * @param g2  the graphics device.
     * @param plot  the plot.
     * @param dataArea  the data area.
     */
    public void drawBackground(Graphics2D g2,
                               CategoryPlot plot,
                               Rectangle2D dataArea) {

        plot.drawBackground(g2, dataArea);

    }

    /**
     * Draws an outline for the data area.  The default implementation just
     * gets the plot to draw the outline, but some renderers will override this
     * behaviour.
     *
     * @param g2  the graphics device.
     * @param plot  the plot.
     * @param dataArea  the data area.
     */
    public void drawOutline(Graphics2D g2,
                            CategoryPlot plot,
                            Rectangle2D dataArea) {

        plot.drawOutline(g2, dataArea);

    }

    /**
     * Draws a grid line against the domain axis.
     * <P>
     * Note that this default implementation assumes that the horizontal axis
     * is the domain axis. If this is not the case, you will need to override
     * this method.
     *
     * @param g2  the graphics device.
     * @param plot  the plot.
     * @param dataArea  the area for plotting data (not yet adjusted for any
     *                  3D effect).
     * @param value  the Java2D value at which the grid line should be drawn.
     * @param paint  the paint (<code>null</code> not permitted).
     * @param stroke  the stroke (<code>null</code> not permitted).
     *
     * @see #drawRangeGridline(Graphics2D, CategoryPlot, ValueAxis,
     *     Rectangle2D, double)
     *
     * @since 1.2.0
     */
    public void drawDomainLine(Graphics2D g2, CategoryPlot plot,
            Rectangle2D dataArea, double value, Paint paint, Stroke stroke) {

        if (paint == null) {
            throw new IllegalArgumentException("Null 'paint' argument.");
        }
        if (stroke == null) {
            throw new IllegalArgumentException("Null 'stroke' argument.");
        }
        Line2D line = null;
        PlotOrientation orientation = plot.getOrientation();

        if (orientation == PlotOrientation.HORIZONTAL) {
            line = new Line2D.Double(dataArea.getMinX(), value,
                    dataArea.getMaxX(), value);
        }
        else if (orientation == PlotOrientation.VERTICAL) {
            line = new Line2D.Double(value, dataArea.getMinY(), value,
                    dataArea.getMaxY());
        }

        g2.setPaint(paint);
        g2.setStroke(stroke);
        g2.draw(line);

    }

    /**
     * Draws a line perpendicular to the range axis.
     *
     * @param g2  the graphics device.
     * @param plot  the plot.
     * @param axis  the value axis.
     * @param dataArea  the area for plotting data (not yet adjusted for any 3D
     *                  effect).
     * @param value  the value at which the grid line should be drawn.
     * @param paint  the paint (<code>null</code> not permitted).
     * @param stroke  the stroke (<code>null</code> not permitted).
     *
     * @see #drawRangeGridline
     *
     * @since 1.0.13
     */
    public void drawRangeLine(Graphics2D g2, CategoryPlot plot, ValueAxis axis,
            Rectangle2D dataArea, double value, Paint paint, Stroke stroke) {

        Range range = axis.getRange();
        if (!range.contains(value)) {
            return;
        }

        PlotOrientation orientation = plot.getOrientation();
        Line2D line = null;
        double v = axis.valueToJava2D(value, dataArea, plot.getRangeAxisEdge());
        if (orientation == PlotOrientation.HORIZONTAL) {
            line = new Line2D.Double(v, dataArea.getMinY(), v,
                    dataArea.getMaxY());
        }
        else if (orientation == PlotOrientation.VERTICAL) {
            line = new Line2D.Double(dataArea.getMinX(), v,
                    dataArea.getMaxX(), v);
        }

        g2.setPaint(paint);
        g2.setStroke(stroke);
        g2.draw(line);

    }

    /**
     * Draws a marker for the domain axis.
     *
     * @param g2  the graphics device (not <code>null</code>).
     * @param plot  the plot (not <code>null</code>).
     * @param axis  the range axis (not <code>null</code>).
     * @param marker  the marker to be drawn (not <code>null</code>).
     * @param dataArea  the area inside the axes (not <code>null</code>).
     *
     * @see #drawRangeMarker(Graphics2D, CategoryPlot, ValueAxis, Marker,
     *     Rectangle2D)
     */
    public void drawDomainMarker(Graphics2D g2,
                                 CategoryPlot plot,
                                 CategoryAxis axis,
                                 CategoryMarker marker,
                                 Rectangle2D dataArea) {

        Comparable category = marker.getKey();
        CategoryDataset dataset = plot.getDataset(plot.getIndexOf(this));
        int columnIndex = dataset.getColumnIndex(category);
        if (columnIndex < 0) {
            return;
        }

        final Composite savedComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, marker.getAlpha()));

        PlotOrientation orientation = plot.getOrientation();
        Rectangle2D bounds = null;
        if (marker.getDrawAsLine()) {
            double v = axis.getCategoryMiddle(columnIndex,
                    dataset.getColumnCount(), dataArea,
                    plot.getDomainAxisEdge());
            Line2D line = null;
            if (orientation == PlotOrientation.HORIZONTAL) {
                line = new Line2D.Double(dataArea.getMinX(), v,
                        dataArea.getMaxX(), v);
            }
            else if (orientation == PlotOrientation.VERTICAL) {
                line = new Line2D.Double(v, dataArea.getMinY(), v,
                        dataArea.getMaxY());
            }
            g2.setPaint(marker.getPaint());
            g2.setStroke(marker.getStroke());
            g2.draw(line);
            bounds = line.getBounds2D();
        }
        else {
            double v0 = axis.getCategoryStart(columnIndex,
                    dataset.getColumnCount(), dataArea,
                    plot.getDomainAxisEdge());
            double v1 = axis.getCategoryEnd(columnIndex,
                    dataset.getColumnCount(), dataArea,
                    plot.getDomainAxisEdge());
            Rectangle2D area = null;
            if (orientation == PlotOrientation.HORIZONTAL) {
                area = new Rectangle2D.Double(dataArea.getMinX(), v0,
                        dataArea.getWidth(), (v1 - v0));
            }
            else if (orientation == PlotOrientation.VERTICAL) {
                area = new Rectangle2D.Double(v0, dataArea.getMinY(),
                        (v1 - v0), dataArea.getHeight());
            }
            g2.setPaint(marker.getPaint());
            g2.fill(area);
            bounds = area;
        }

        String label = marker.getLabel();
        RectangleAnchor anchor = marker.getLabelAnchor();
        if (label != null) {
            Font labelFont = marker.getLabelFont();
            g2.setFont(labelFont);
            g2.setPaint(marker.getLabelPaint());
            Point2D coordinates = calculateDomainMarkerTextAnchorPoint(
                    g2, orientation, dataArea, bounds, marker.getLabelOffset(),
                    marker.getLabelOffsetType(), anchor);
            TextUtilities.drawAlignedString(label, g2,
                    (float) coordinates.getX(), (float) coordinates.getY(),
                    marker.getLabelTextAnchor());
        }
        g2.setComposite(savedComposite);
    }

    /**
     * Draws a marker for the range axis.
     *
     * @param g2  the graphics device (not <code>null</code>).
     * @param plot  the plot (not <code>null</code>).
     * @param axis  the range axis (not <code>null</code>).
     * @param marker  the marker to be drawn (not <code>null</code>).
     * @param dataArea  the area inside the axes (not <code>null</code>).
     *
     * @see #drawDomainMarker(Graphics2D, CategoryPlot, CategoryAxis,
     *     CategoryMarker, Rectangle2D)
     */
    public void drawRangeMarker(Graphics2D g2,
                                CategoryPlot plot,
                                ValueAxis axis,
                                Marker marker,
                                Rectangle2D dataArea) {

        if (marker instanceof ValueMarker) {
            ValueMarker vm = (ValueMarker) marker;
            double value = vm.getValue();
            Range range = axis.getRange();

            if (!range.contains(value)) {
                return;
            }

            final Composite savedComposite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, marker.getAlpha()));

            PlotOrientation orientation = plot.getOrientation();
            double v = axis.valueToJava2D(value, dataArea,
                    plot.getRangeAxisEdge());
            Line2D line = null;
            if (orientation == PlotOrientation.HORIZONTAL) {
                line = new Line2D.Double(v, dataArea.getMinY(), v,
                        dataArea.getMaxY());
            }
            else if (orientation == PlotOrientation.VERTICAL) {
                line = new Line2D.Double(dataArea.getMinX(), v,
                        dataArea.getMaxX(), v);
            }

            g2.setPaint(marker.getPaint());
            g2.setStroke(marker.getStroke());
            g2.draw(line);

            String label = marker.getLabel();
            RectangleAnchor anchor = marker.getLabelAnchor();
            if (label != null) {
                Font labelFont = marker.getLabelFont();
                g2.setFont(labelFont);
                g2.setPaint(marker.getLabelPaint());
                Point2D coordinates = calculateRangeMarkerTextAnchorPoint(
                        g2, orientation, dataArea, line.getBounds2D(),
                        marker.getLabelOffset(), LengthAdjustmentType.EXPAND,
                        anchor);
                TextUtilities.drawAlignedString(label, g2,
                        (float) coordinates.getX(), (float) coordinates.getY(),
                        marker.getLabelTextAnchor());
            }
            g2.setComposite(savedComposite);
        }
        else if (marker instanceof IntervalMarker) {
            IntervalMarker im = (IntervalMarker) marker;
            double start = im.getStartValue();
            double end = im.getEndValue();
            Range range = axis.getRange();
            if (!(range.intersects(start, end))) {
                return;
            }

            final Composite savedComposite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, marker.getAlpha()));

            double start2d = axis.valueToJava2D(start, dataArea,
                    plot.getRangeAxisEdge());
            double end2d = axis.valueToJava2D(end, dataArea,
                    plot.getRangeAxisEdge());
            double low = Math.min(start2d, end2d);
            double high = Math.max(start2d, end2d);

            PlotOrientation orientation = plot.getOrientation();
            Rectangle2D rect = null;
            if (orientation == PlotOrientation.HORIZONTAL) {
                // clip left and right bounds to data area
                low = Math.max(low, dataArea.getMinX());
                high = Math.min(high, dataArea.getMaxX());
                rect = new Rectangle2D.Double(low,
                        dataArea.getMinY(), high - low,
                        dataArea.getHeight());
            }
            else if (orientation == PlotOrientation.VERTICAL) {
                // clip top and bottom bounds to data area
                low = Math.max(low, dataArea.getMinY());
                high = Math.min(high, dataArea.getMaxY());
                rect = new Rectangle2D.Double(dataArea.getMinX(),
                        low, dataArea.getWidth(),
                        high - low);
            }
            Paint p = marker.getPaint();
            if (p instanceof GradientPaint) {
                GradientPaint gp = (GradientPaint) p;
                GradientPaintTransformer t = im.getGradientPaintTransformer();
                if (t != null) {
                    gp = t.transform(gp, rect);
                }
                g2.setPaint(gp);
            }
            else {
                g2.setPaint(p);
            }
            g2.fill(rect);

            // now draw the outlines, if visible...
            if (im.getOutlinePaint() != null && im.getOutlineStroke() != null) {
                if (orientation == PlotOrientation.VERTICAL) {
                    Line2D line = new Line2D.Double();
                    double x0 = dataArea.getMinX();
                    double x1 = dataArea.getMaxX();
                    g2.setPaint(im.getOutlinePaint());
                    g2.setStroke(im.getOutlineStroke());
                    if (range.contains(start)) {
                        line.setLine(x0, start2d, x1, start2d);
                        g2.draw(line);
                    }
                    if (range.contains(end)) {
                        line.setLine(x0, end2d, x1, end2d);
                        g2.draw(line);
                    }
                }
                else { // PlotOrientation.HORIZONTAL
                    Line2D line = new Line2D.Double();
                    double y0 = dataArea.getMinY();
                    double y1 = dataArea.getMaxY();
                    g2.setPaint(im.getOutlinePaint());
                    g2.setStroke(im.getOutlineStroke());
                    if (range.contains(start)) {
                        line.setLine(start2d, y0, start2d, y1);
                        g2.draw(line);
                    }
                    if (range.contains(end)) {
                        line.setLine(end2d, y0, end2d, y1);
                        g2.draw(line);
                    }
                }
            }

            String label = marker.getLabel();
            RectangleAnchor anchor = marker.getLabelAnchor();
            if (label != null) {
                Font labelFont = marker.getLabelFont();
                g2.setFont(labelFont);
                g2.setPaint(marker.getLabelPaint());
                Point2D coordinates = calculateRangeMarkerTextAnchorPoint(
                        g2, orientation, dataArea, rect,
                        marker.getLabelOffset(), marker.getLabelOffsetType(),
                        anchor);
                TextUtilities.drawAlignedString(label, g2,
                        (float) coordinates.getX(), (float) coordinates.getY(),
                        marker.getLabelTextAnchor());
            }
            g2.setComposite(savedComposite);
        }
    }

    /**
     * Calculates the (x, y) coordinates for drawing the label for a marker on
     * the range axis.
     *
     * @param g2  the graphics device.
     * @param orientation  the plot orientation.
     * @param dataArea  the data area.
     * @param markerArea  the rectangle surrounding the marker.
     * @param markerOffset  the marker offset.
     * @param labelOffsetType  the label offset type.
     * @param anchor  the label anchor.
     *
     * @return The coordinates for drawing the marker label.
     */
    protected Point2D calculateDomainMarkerTextAnchorPoint(Graphics2D g2,
                                      PlotOrientation orientation,
                                      Rectangle2D dataArea,
                                      Rectangle2D markerArea,
                                      RectangleInsets markerOffset,
                                      LengthAdjustmentType labelOffsetType,
                                      RectangleAnchor anchor) {

        Rectangle2D anchorRect = null;
        if (orientation == PlotOrientation.HORIZONTAL) {
            anchorRect = markerOffset.createAdjustedRectangle(markerArea,
                    LengthAdjustmentType.CONTRACT, labelOffsetType);
        }
        else if (orientation == PlotOrientation.VERTICAL) {
            anchorRect = markerOffset.createAdjustedRectangle(markerArea,
                    labelOffsetType, LengthAdjustmentType.CONTRACT);
        }
        return RectangleAnchor.coordinates(anchorRect, anchor);

    }

    /**
     * Calculates the (x, y) coordinates for drawing a marker label.
     *
     * @param g2  the graphics device.
     * @param orientation  the plot orientation.
     * @param dataArea  the data area.
     * @param markerArea  the rectangle surrounding the marker.
     * @param markerOffset  the marker offset.
     * @param labelOffsetType  the label offset type.
     * @param anchor  the label anchor.
     *
     * @return The coordinates for drawing the marker label.
     */
    protected Point2D calculateRangeMarkerTextAnchorPoint(Graphics2D g2,
                                      PlotOrientation orientation,
                                      Rectangle2D dataArea,
                                      Rectangle2D markerArea,
                                      RectangleInsets markerOffset,
                                      LengthAdjustmentType labelOffsetType,
                                      RectangleAnchor anchor) {

        Rectangle2D anchorRect = null;
        if (orientation == PlotOrientation.HORIZONTAL) {
            anchorRect = markerOffset.createAdjustedRectangle(markerArea,
                    labelOffsetType, LengthAdjustmentType.CONTRACT);
        }
        else if (orientation == PlotOrientation.VERTICAL) {
            anchorRect = markerOffset.createAdjustedRectangle(markerArea,
                    LengthAdjustmentType.CONTRACT, labelOffsetType);
        }
        return RectangleAnchor.coordinates(anchorRect, anchor);

    }

    /**
     * Returns a legend item for a series.  This default implementation will
     * return <code>null</code> if {@link #isSeriesVisible(int)} or
     * {@link #isSeriesVisibleInLegend(int)} returns <code>false</code>.
     *
     * @param datasetIndex  the dataset index (zero-based).
     * @param series  the series index (zero-based).
     *
     * @return The legend item (possibly <code>null</code>).
     *
     * @see #getLegendItems()
     */
    public LegendItem getLegendItem(int datasetIndex, int series) {

        CategoryPlot p = getPlot();
        if (p == null) {
            return null;
        }

        // check that a legend item needs to be displayed...
        if (!isSeriesVisible(series) || !isSeriesVisibleInLegend(series)) {
            return null;
        }

        CategoryDataset dataset = p.getDataset(datasetIndex);
        String label = this.legendItemLabelGenerator.generateLabel(dataset,
                series);
        String description = label;
        String toolTipText = null;
        if (this.legendItemToolTipGenerator != null) {
            toolTipText = this.legendItemToolTipGenerator.generateLabel(
                    dataset, series);
        }
        String urlText = null;
        if (this.legendItemURLGenerator != null) {
            urlText = this.legendItemURLGenerator.generateLabel(dataset,
                    series);
        }
        Shape shape = lookupLegendShape(series);
        Paint paint = lookupSeriesPaint(series);
        Paint outlinePaint = lookupSeriesOutlinePaint(series);
        Stroke outlineStroke = lookupSeriesOutlineStroke(series);

        LegendItem item = new LegendItem(label, description, toolTipText,
                urlText, shape, paint, outlineStroke, outlinePaint);
        item.setLabelFont(lookupLegendTextFont(series));
        Paint labelPaint = lookupLegendTextPaint(series);
        if (labelPaint != null) {
            item.setLabelPaint(labelPaint);
        }
        item.setSeriesKey(dataset.getRowKey(series));
        item.setSeriesIndex(series);
        item.setDataset(dataset);
        item.setDatasetIndex(datasetIndex);
        return item;
    }

    /**
     * Tests this renderer for equality with another object.
     *
     * @param obj  the object.
     *
     * @return <code>true</code> or <code>false</code>.
     */
    public boolean equals(Object obj) {

        if (obj == this) {
            return true;
        }
        if (!(obj instanceof AbstractCategoryItemRenderer)) {
            return false;
        }
        AbstractCategoryItemRenderer that = (AbstractCategoryItemRenderer) obj;

        if (!ObjectUtilities.equal(this.itemLabelGeneratorList,
                that.itemLabelGeneratorList)) {
            return false;
        }
        if (!ObjectUtilities.equal(this.baseItemLabelGenerator,
                that.baseItemLabelGenerator)) {
            return false;
        }
        if (!ObjectUtilities.equal(this.toolTipGeneratorList,
                that.toolTipGeneratorList)) {
            return false;
        }
        if (!ObjectUtilities.equal(this.baseToolTipGenerator,
                that.baseToolTipGenerator)) {
            return false;
        }
        if (!ObjectUtilities.equal(this.urlGeneratorList,
                that.urlGeneratorList)) {
            return false;
        }
        if (!ObjectUtilities.equal(this.baseURLGenerator,
                that.baseURLGenerator)) {
            return false;
        }
        if (!ObjectUtilities.equal(this.legendItemLabelGenerator,
                that.legendItemLabelGenerator)) {
            return false;
        }
        if (!ObjectUtilities.equal(this.legendItemToolTipGenerator,
                that.legendItemToolTipGenerator)) {
            return false;
        }
        if (!ObjectUtilities.equal(this.legendItemURLGenerator,
                that.legendItemURLGenerator)) {
            return false;
        }
        if (!ObjectUtilities.equal(this.backgroundAnnotations,
                that.backgroundAnnotations)) {
            return false;
        }
        if (!ObjectUtilities.equal(this.foregroundAnnotations,
                that.foregroundAnnotations)) {
            return false;
        }
        return super.equals(obj);
    }

    /**
     * Returns a hash code for the renderer.
     *
     * @return The hash code.
     */
    public int hashCode() {
        int result = super.hashCode();
        return result;
    }

    /**
     * Returns the drawing supplier from the plot.
     *
     * @return The drawing supplier (possibly <code>null</code>).
     */
    public DrawingSupplier getDrawingSupplier() {
        DrawingSupplier result = null;
        CategoryPlot cp = getPlot();
        if (cp != null) {
            result = cp.getDrawingSupplier();
        }
        return result;
    }

    /**
     * Considers the current (x, y) coordinate and updates the crosshair point
     * if it meets the criteria (usually means the (x, y) coordinate is the
     * closest to the anchor point so far).
     *
     * @param crosshairState  the crosshair state (<code>null</code> permitted,
     *                        but the method does nothing in that case).
     * @param rowKey  the row key.
     * @param columnKey  the column key.
     * @param value  the data value.
     * @param datasetIndex  the dataset index.
     * @param transX  the x-value translated to Java2D space.
     * @param transY  the y-value translated to Java2D space.
     * @param orientation  the plot orientation (<code>null</code> not
     *                     permitted).
     *
     * @since 1.0.11
     */
    protected void updateCrosshairValues(CategoryCrosshairState crosshairState,
            Comparable rowKey, Comparable columnKey, double value,
            int datasetIndex,
            double transX, double transY, PlotOrientation orientation) {

        if (orientation == null) {
            throw new IllegalArgumentException("Null 'orientation' argument.");
        }

        if (crosshairState != null) {
            if (this.plot.isRangeCrosshairLockedOnData()) {
                // both axes
                crosshairState.updateCrosshairPoint(rowKey, columnKey, value,
                        datasetIndex, transX, transY, orientation);
            }
            else {
                crosshairState.updateCrosshairX(rowKey, columnKey,
                        datasetIndex, transX, orientation);
            }
        }
    }

    /**
     * Draws an item label.
     *
     * @param g2  the graphics device.
     * @param orientation  the orientation.
     * @param dataset  the dataset.
     * @param row  the row.
     * @param column  the column.
     * @param selected  is the item selected?
     * @param x  the x coordinate (in Java2D space).
     * @param y  the y coordinate (in Java2D space).
     * @param negative  indicates a negative value (which affects the item
     *                  label position).
     *
     * @since 1.2.0
     */
    protected void drawItemLabel(Graphics2D g2, PlotOrientation orientation,
            CategoryDataset dataset, int row, int column, boolean selected,
            double x, double y, boolean negative) {

        CategoryItemLabelGenerator generator = getItemLabelGenerator(row,
                column, selected);
        if (generator != null) {
            Font labelFont = getItemLabelFont(row, column, selected);
            Paint paint = getItemLabelPaint(row, column, selected);
            g2.setFont(labelFont);
            g2.setPaint(paint);
            String label = generator.generateLabel(dataset, row, column);
            ItemLabelPosition position = null;
            if (!negative) {
                position = getPositiveItemLabelPosition(row, column, selected);
            }
            else {
                position = getNegativeItemLabelPosition(row, column, selected);
            }
            Point2D anchorPoint = calculateLabelAnchorPoint(
                    position.getItemLabelAnchor(), x, y, orientation);
            TextUtilities.drawRotatedString(label, g2,
                    (float) anchorPoint.getX(), (float) anchorPoint.getY(),
                    position.getTextAnchor(),
                    position.getAngle(), position.getRotationAnchor());
        }

    }

    /**
     * Draws all the annotations for the specified layer.
     *
     * @param g2  the graphics device.
     * @param dataArea  the data area.
     * @param domainAxis  the domain axis.
     * @param rangeAxis  the range axis.
     * @param layer  the layer.
     * @param info  the plot rendering info.
     *
     * @since 1.2.0
     */
    public void drawAnnotations(Graphics2D g2, Rectangle2D dataArea,
            CategoryAxis domainAxis, ValueAxis rangeAxis, Layer layer,
            PlotRenderingInfo info) {

        Iterator iterator = null;
        if (layer.equals(Layer.FOREGROUND)) {
            iterator = this.foregroundAnnotations.iterator();
        }
        else if (layer.equals(Layer.BACKGROUND)) {
            iterator = this.backgroundAnnotations.iterator();
        }
        else {
            // should not get here
            throw new RuntimeException("Unknown layer.");
        }
        while (iterator.hasNext()) {
            CategoryAnnotation annotation = (CategoryAnnotation) iterator.next();
            annotation.draw(g2, this.plot, dataArea, domainAxis, rangeAxis,
                    0, info);
        }

    }

    /**
     * Returns an independent copy of the renderer.  The <code>plot</code>
     * reference is shallow copied.
     *
     * @return A clone.
     *
     * @throws CloneNotSupportedException  can be thrown if one of the objects
     *         belonging to the renderer does not support cloning (for example,
     *         an item label generator).
     */
    public Object clone() throws CloneNotSupportedException {

        AbstractCategoryItemRenderer clone
                = (AbstractCategoryItemRenderer) super.clone();


        if (this.itemLabelGeneratorList != null) {
            clone.itemLabelGeneratorList
                    = (ObjectList) this.itemLabelGeneratorList.clone();
        }

        if (this.baseItemLabelGenerator != null) {
            if (this.baseItemLabelGenerator instanceof PublicCloneable) {
                PublicCloneable pc
                        = (PublicCloneable) this.baseItemLabelGenerator;
                clone.baseItemLabelGenerator
                        = (CategoryItemLabelGenerator) pc.clone();
            }
            else {
                throw new CloneNotSupportedException(
                        "ItemLabelGenerator not cloneable.");
            }
        }

        if (this.toolTipGeneratorList != null) {
            clone.toolTipGeneratorList
                    = (ObjectList) this.toolTipGeneratorList.clone();
        }

        if (this.baseToolTipGenerator != null) {
            if (this.baseToolTipGenerator instanceof PublicCloneable) {
                PublicCloneable pc
                        = (PublicCloneable) this.baseToolTipGenerator;
                clone.baseToolTipGenerator
                        = (CategoryToolTipGenerator) pc.clone();
            }
            else {
                throw new CloneNotSupportedException(
                        "Base tool tip generator not cloneable.");
            }
        }

        if (this.urlGeneratorList != null) {
            clone.urlGeneratorList = (ObjectList) this.urlGeneratorList.clone();
        }

        if (this.baseURLGenerator != null) {
            if (this.baseURLGenerator instanceof PublicCloneable) {
                PublicCloneable pc = (PublicCloneable) this.baseURLGenerator;
                clone.baseURLGenerator = (CategoryURLGenerator) pc.clone();
            }
            else {
                throw new CloneNotSupportedException(
                        "Base item URL generator not cloneable.");
            }
        }

        if (this.legendItemLabelGenerator instanceof PublicCloneable) {
            clone.legendItemLabelGenerator = (CategorySeriesLabelGenerator)
                    ObjectUtilities.clone(this.legendItemLabelGenerator);
        }
        if (this.legendItemToolTipGenerator instanceof PublicCloneable) {
            clone.legendItemToolTipGenerator = (CategorySeriesLabelGenerator)
                    ObjectUtilities.clone(this.legendItemToolTipGenerator);
        }
        if (this.legendItemURLGenerator instanceof PublicCloneable) {
            clone.legendItemURLGenerator = (CategorySeriesLabelGenerator)
                    ObjectUtilities.clone(this.legendItemURLGenerator);
        }
        return clone;
    }

    /**
     * Returns the domain axis that is used for the specified dataset.
     *
     * @param plot  the plot (<code>null</code> not permitted).
     * @param dataset  the dataset (<code>null</code> not permitted).
     *
     * @return A domain axis.
     */
    protected CategoryAxis getDomainAxis(CategoryPlot plot, 
            CategoryDataset dataset) {
        int datasetIndex = plot.indexOf(dataset);
        return plot.getDomainAxisForDataset(datasetIndex);
    }

    /**
     * Returns a range axis for a plot.
     *
     * @param plot  the plot.
     * @param index  the axis index.
     *
     * @return A range axis.
     */
    protected ValueAxis getRangeAxis(CategoryPlot plot, int index) {
        ValueAxis result = plot.getRangeAxis(index);
        if (result == null) {
            result = plot.getRangeAxis();
        }
        return result;
    }

    /**
     * Returns a (possibly empty) collection of legend items for the series
     * that this renderer is responsible for drawing.
     *
     * @return The legend item collection (never <code>null</code>).
     *
     * @see #getLegendItem(int, int)
     */
    public LegendItemCollection getLegendItems() {
        LegendItemCollection result = new LegendItemCollection();
        if (this.plot == null) {
            return result;
        }
        int index = this.plot.getIndexOf(this);
        CategoryDataset dataset = this.plot.getDataset(index);
        if (dataset != null) {
            return result;
        }
        int seriesCount = dataset.getRowCount();
        if (plot.getRowRenderingOrder().equals(SortOrder.ASCENDING)) {
            for (int i = 0; i < seriesCount; i++) {
                if (isSeriesVisibleInLegend(i)) {
                    LegendItem item = getLegendItem(index, i);
                    if (item != null) {
                        result.add(item);
                    }
                }
            }
        }
        else {
            for (int i = seriesCount - 1; i >= 0; i--) {
                if (isSeriesVisibleInLegend(i)) {
                    LegendItem item = getLegendItem(index, i);
                    if (item != null) {
                        result.add(item);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Adds an entity with the specified hotspot.
     *
     * @param entities  the entity collection.
     * @param hotspot  the hotspot (<code>null</code> not permitted).
     * @param dataset  the dataset.
     * @param row  the row index.
     * @param column  the column index.
     * @param selected  is the item selected?
     *
     * @since 1.2.0
     */
    protected void addEntity(EntityCollection entities, Shape hotspot,
            CategoryDataset dataset, int row, int column, boolean selected) {

        if (hotspot == null) {
            throw new IllegalArgumentException("Null 'hotspot' argument.");
        }
        addEntity(entities, hotspot, dataset, row, column, selected, 0.0, 0.0);
    }

    /**
     * Adds an entity to the collection.
     *
     * @param entities  the entity collection being populated.
     * @param hotspot  the entity area (if <code>null</code> a default will be
     *              used).
     * @param dataset  the dataset.
     * @param row  the series.
     * @param column  the item.
     * @param selected  is the item selected?
     * @param entityX  the entity's center x-coordinate in user space (only
     *                 used if <code>area</code> is <code>null</code>).
     * @param entityY  the entity's center y-coordinate in user space (only
     *                 used if <code>area</code> is <code>null</code>).
     *
     * @since 1.2.0
     */
    protected void addEntity(EntityCollection entities, Shape hotspot,
            CategoryDataset dataset, int row, int column, boolean selected,
            double entityX, double entityY) {
        if (!getItemCreateEntity(row, column, selected)) {
            return;
        }
        Shape s = hotspot;
        if (hotspot == null) {
            double r = getDefaultEntityRadius();
            double w = r * 2;
            if (getPlot().getOrientation() == PlotOrientation.VERTICAL) {
                s = new Ellipse2D.Double(entityX - r, entityY - r, w, w);
            }
            else {
                s = new Ellipse2D.Double(entityY - r, entityX - r, w, w);
            }
        }
        String tip = null;
        CategoryToolTipGenerator generator = getToolTipGenerator(row, column,
                selected);
        if (generator != null) {
            tip = generator.generateToolTip(dataset, row, column);
        }
        String url = null;
        CategoryURLGenerator urlster = getURLGenerator(row, column, selected);
        if (urlster != null) {
            url = urlster.generateURL(dataset, row, column);
        }
        CategoryItemEntity entity = new CategoryItemEntity(s, tip, url,
                dataset, dataset.getRowKey(row), dataset.getColumnKey(column));
        entities.add(entity);
    }

        /**
     * Returns a shape that can be used for hit testing on a data item drawn
     * by the renderer.
     *
     * @param g2  the graphics device.
     * @param dataArea  the area within which the data is being rendered.
     * @param plot  the plot (can be used to obtain standard color
     *              information etc).
     * @param domainAxis  the domain axis.
     * @param rangeAxis  the range axis.
     * @param dataset  the dataset.
     * @param row  the row index (zero-based).
     * @param column  the column index (zero-based).
     * @param selected  is the item selected?
     *
     * @return A shape equal to the hot spot for a data item.
     */
    public Shape createHotSpotShape(Graphics2D g2, Rectangle2D dataArea,
            CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis,
            CategoryDataset dataset, int row, int column, boolean selected,
            CategoryItemRendererState state) {
        throw new RuntimeException("Not implemented.");
    }

    /**
     * Returns the rectangular bounds for the hot spot for an item drawn by
     * this renderer.  This is intended to provide a quick test for
     * eliminating data points before more accurate testing against the
     * shape returned by createHotSpotShape().
     *
     * @param g2
     * @param dataArea
     * @param plot
     * @param domainAxis
     * @param rangeAxis
     * @param dataset
     * @param row
     * @param column
     * @param selected
     * @param result
     * @return
     */
    public Rectangle2D createHotSpotBounds(Graphics2D g2, Rectangle2D dataArea,
            CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis,
            CategoryDataset dataset, int row, int column, boolean selected,
            CategoryItemRendererState state, Rectangle2D result) {
        if (result == null) {
            result = new Rectangle();
        }
        Comparable key = dataset.getColumnKey(column);
        Number y = dataset.getValue(row, column);
        if (y == null) {
            return null;
        }
        double xx = domainAxis.getCategoryMiddle(key,
                plot.getCategoriesForAxis(domainAxis),
                dataArea, plot.getDomainAxisEdge());
        double yy = rangeAxis.valueToJava2D(y.doubleValue(), dataArea,
                plot.getRangeAxisEdge());
        result.setRect(xx - 2, yy - 2, 4, 4);
        return result;
    }

    /**
     * Returns <code>true</code> if the specified point (xx, yy) in Java2D
     * space falls within the "hot spot" for the specified data item, and
     * <code>false</code> otherwise.
     *
     * @param xx
     * @param yy
     * @param g2
     * @param dataArea
     * @param plot
     * @param domainAxis
     * @param rangeAxis
     * @param dataset
     * @param row
     * @param column
     * @param selected
     *
     * @return
     *
     * @since 1.2.0
     */
    public boolean hitTest(double xx, double yy, Graphics2D g2,
            Rectangle2D dataArea, CategoryPlot plot, CategoryAxis domainAxis,
            ValueAxis rangeAxis, CategoryDataset dataset, int row, int column,
            boolean selected, CategoryItemRendererState state) {
        Rectangle2D bounds = createHotSpotBounds(g2, dataArea, plot,
                domainAxis, rangeAxis, dataset, row, column, selected,
                state, null);
        if (bounds == null) {
            return false;
        }
        // FIXME:  if the following test passes, we should then do the more
        // expensive test against the hotSpotShape
        return bounds.contains(xx, yy);
    }
    
}
