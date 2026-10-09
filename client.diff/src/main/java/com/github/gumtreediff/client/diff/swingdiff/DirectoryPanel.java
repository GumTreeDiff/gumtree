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
 * Copyright 2022 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.client.diff.swingdiff;

import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.client.diff.AbstractDiffClient;
import com.github.gumtreediff.gen.TreeGenerators;
import com.github.gumtreediff.io.DirectoryComparator;
import com.github.gumtreediff.utils.Pair;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class DirectoryPanel extends JPanel implements ListSelectionListener {
    private final DirectoryComparator comparator;
    private final JList<Pair<File, File>> listModified;
    private final AbstractDiffClient client;

    public DirectoryPanel(String src, String dst, AbstractDiffClient client) {
        super(new GridLayout(3, 1));
        this.client = client;
        this.comparator = new DirectoryComparator(src, dst);
        comparator.compare();

        Pair<File, File>[] modifiedFilesArray = new Pair[comparator.getModifiedFiles().size()];
        comparator.getModifiedFiles().toArray(modifiedFilesArray);
        listModified = new JList<>(modifiedFilesArray);
        listModified.setCellRenderer(new PairFileCellRenderer());
        listModified.addListSelectionListener(this);
        listModified.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane panModified = new JScrollPane(listModified);
        this.add(panModified);

        File[] addedFilesArray = new File[comparator.getAddedFiles().size()];
        comparator.getAddedFiles().toArray(addedFilesArray);
        JList<File> listAdded = new JList<>(addedFilesArray);
        listAdded.setSelectionModel(new DisabledItemSelectionModel());
        listAdded.setBackground(new Color(0, 255, 0, 128));
        listAdded.setCellRenderer(new FileCellRenderer(comparator.getDst()));
        JScrollPane panAdded = new JScrollPane(listAdded);
        this.add(panAdded);

        File[] deletedFilesArray = new File[comparator.getDeletedFiles().size()];
        comparator.getDeletedFiles().toArray(deletedFilesArray);
        JList<File> listDeleted = new JList<>(deletedFilesArray);
        listDeleted.setBackground(new Color(255, 0, 0, 128));
        listDeleted.setCellRenderer(new FileCellRenderer(comparator.getSrc()));
        listDeleted.setSelectionModel(new DisabledItemSelectionModel());
        JScrollPane panDeleted = new JScrollPane(listDeleted);
        this.add(panDeleted);

        this.setPreferredSize(new Dimension(1024, 768));
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting())
            return;

        Pair<File, File> files = listModified.getSelectedValue();
        
        if (!TreeGenerators.getInstance().hasGeneratorForFile(files.first.getAbsolutePath()))
            return;
        Diff diff = null;
        try {
            diff = client.getDiff(files.first.getAbsolutePath(), files.second.getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        final Diff theDiff = diff;
        if (diff == null)
            return;

        javax.swing.SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Diff");
            frame.add(new MappingsPanel(files.first.getAbsolutePath(), files.second.getAbsolutePath(), theDiff));
            frame.pack();
            frame.setVisible(true);
        });
    }

    private class PairFileCellRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            Component res = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Pair<File, File> files = (Pair<File, File>) value;
            String fileName = comparator.getSrc().toAbsolutePath()
                    .relativize(files.first.toPath().toAbsolutePath()).toString();
            setText((fileName));
            return res;
        }
    }

    private class FileCellRenderer extends DefaultListCellRenderer {
        private Path root;

        public FileCellRenderer(Path root) {
            this.root = root;
        }

        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            Component res = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            File file = (File) value;
            String fileName = root.toAbsolutePath()
                    .relativize(file.toPath().toAbsolutePath()).toString();
            setText((fileName));
            return res;
        }
    }

    private class DisabledItemSelectionModel extends DefaultListSelectionModel {
        @Override
        public void setSelectionInterval(int index0, int index1) {
            super.setSelectionInterval(-1, -1);
        }

        @Override
        public void addSelectionInterval(int index0, int index1)  {
            super.setSelectionInterval(-1, -1);
        }
    }
}
