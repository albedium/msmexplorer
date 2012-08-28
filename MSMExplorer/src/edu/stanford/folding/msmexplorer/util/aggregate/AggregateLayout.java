/*
 * Copyright (C) 2012 Stanford University
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package edu.stanford.folding.msmexplorer.util.aggregate;

import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import prefuse.Visualization;
import prefuse.action.layout.Layout;
import prefuse.util.GraphicsLib;
import prefuse.visual.AggregateItem;
import prefuse.visual.AggregateTable;
import prefuse.visual.VisualItem;

/**
 * Layout algorithm that computes a convex hull surrounding
 * aggregate items and saves it in the "_polygon" field.
 * 
 * @author Jeffrey Heer
 */
public class AggregateLayout extends Layout {
    
    private int m_margin = 5; // convex hull pixel margin
    private double[] m_pts;   // buffer for computing convex hulls
    
    public AggregateLayout(String aggrGroup, Visualization vis) {
	    super(aggrGroup);
	    this.setVisualization(vis);
    }
    
    /**
     * @see edu.berkeley.guir.prefuse.action.Action#run(edu.berkeley.guir.prefuse.ItemRegistry, double)
     */
    public void run(double frac) {
	    
	    try {
		    
		    AggregateTable aggr = (AggregateTable)m_vis.getGroup(m_group);
		    // do we have any  to process?
		    int num = aggr.getTupleCount();
		    if ( num == 0 ) return;
		    
		    // update buffers
		    int maxsz = 0;
		    for ( Iterator aggrs = aggr.tuples(); aggrs.hasNext();  )
			    maxsz = Math.max(maxsz, 4*2*
				    ((AggregateItem)aggrs.next()).getAggregateSize());
		    if ( m_pts == null || maxsz > m_pts.length ) {
			    m_pts = new double[maxsz];
		    }
		    
		    // compute and assign convex hull for each aggregate
		    Iterator aggrs = m_vis.visibleItems(m_group);
		    while ( aggrs.hasNext() ) {
			    AggregateItem aitem = (AggregateItem)aggrs.next();
			    
			    int idx = 0;
			    if ( aitem.getAggregateSize() == 0 ) continue;
			    VisualItem item = null;
			    Iterator iter = aitem.items();
			    while ( iter.hasNext() ) {
				    item = (VisualItem)iter.next();
				    if ( item.isVisible() ) {
					    addPoint(m_pts, idx, item, m_margin);
					    idx += 2*4;
				    }
			    }
			    // if no aggregates are visible, do nothing
			    if ( idx == 0 ) continue;
			    
			    // compute convex hull
			    double[] nhull = GraphicsLib.convexHull(m_pts, idx);
			    
			    // prepare viz attribute array
			    float[]  fhull = (float[])aitem.get(VisualItem.POLYGON);
			    if ( fhull == null || fhull.length < nhull.length )
				    fhull = new float[nhull.length];
			    else if ( fhull.length > nhull.length )
				    fhull[nhull.length] = Float.NaN;
			    
			    // copy hull values
			    for ( int j=0; j<nhull.length; j++ )
				    fhull[j] = (float)nhull[j];
			    aitem.set(VisualItem.POLYGON, fhull);
			    aitem.setValidated(false); // force invalidation
		    }
	    } catch (Exception e) {
		    Logger.getLogger(AggregateLayout.class.getName()).log(Level.WARNING, null, e);
	    }
    }
    
    private static void addPoint(double[] pts, int idx, 
                                 VisualItem item, int growth)
    {
        Rectangle2D b = item.getBounds();
        double minX = (b.getMinX())-growth, minY = (b.getMinY())-growth;
        double maxX = (b.getMaxX())+growth, maxY = (b.getMaxY())+growth;
        pts[idx]   = minX; pts[idx+1] = minY;
        pts[idx+2] = minX; pts[idx+3] = maxY;
        pts[idx+4] = maxX; pts[idx+5] = minY;
        pts[idx+6] = maxX; pts[idx+7] = maxY;
    }
    
} // end of class AggregateLayout
