/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2019 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.client.diff.swingdiff;

import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.client.Register;
import com.github.gumtreediff.client.diff.AbstractDiffClient;

import javax.swing.*;

import java.io.IOException;

@Register(description = "A swing diff client", options = AbstractDiffClient.DiffOptions.class)
public final class SwingDiff extends AbstractDiffClient<AbstractDiffClient.DiffOptions> {

    public SwingDiff(String[] args) {
        super(args);
    }

    @Override
    public void run() throws IOException {
        Diff diff = getDiff();
        javax.swing.SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("GumTree");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new MappingsPanel(opts.srcPath, opts.dstPath, diff));
            frame.pack();
            frame.setVisible(true);
        });
    }

    @Override
    protected DiffOptions newOptions() {
        return new DiffOptions();
    }
}