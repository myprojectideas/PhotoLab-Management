/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.photoslide.search;

import java.io.IOException;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;
import org.photoslide.datamodel.GridCellSelectionModel;
import org.photoslide.datamodel.MediaFile;
import org.photoslide.datamodel.MediaFileLoader;

/**
 *
 * @author selfemp
 */
public class MediaGridCellSearchFactory implements Callback<GridView<MediaFile>, GridCell<MediaFile>> {

    private final SortedList<MediaFile> sortedMediaList;
    private MediaGridCellSR selectedCell;
    private MediaFile selectedMediaFile;
    private final Comparator<MediaFile> stackNameComparator;
    private final ExecutorService executor;
    private final SearchToolsController searchTools;
    private final GridCellSelectionModel selectionModel;
    private final MediaFileLoader fileLoader;

    public MediaGridCellSearchFactory(ExecutorService executor, SearchToolsController controller, SortedList<MediaFile> sortedMediaList) {
        this.sortedMediaList = sortedMediaList;
        this.searchTools = controller;
        stackNameComparator = Comparator.comparing(MediaFile::getStackPos);
        this.executor = executor;
        selectionModel = new GridCellSelectionModel();
        fileLoader = new MediaFileLoader();
    }

    @Override
    public GridCell<MediaFile> call(GridView<MediaFile> p) {
        MediaGridCellSR cell = new MediaGridCellSR();
        cell.setAlignment(Pos.CENTER);
        cell.setEditable(false);
        cell.setOnMouseClicked((t) -> {
            manageGUISelection(t, cell);
            handleGridCellSelection(t);
            t.consume();
        });
        cell.itemProperty().addListener((ov, oldMediaItem, newMediaItem) -> {
            if (newMediaItem != null && oldMediaItem == null) {
                if (newMediaItem.isLoading() == true) {
                    if (newMediaItem.getMediaType() == MediaFile.MediaTypes.IMAGE) {
                        if (isCellVisible(cell)) {
                            fileLoader.loadImage(newMediaItem);
                        }
                    } else {
                        if (isCellVisible(cell)) {
                            fileLoader.loadVideo(newMediaItem);
                        }
                    }
                }
            }
        });
        return cell;
    }

    private void manageGUISelection(MouseEvent t, MediaGridCellSR cell) {
        searchTools.getFullMediaList().stream().filter(c -> c != null && c.isSelected() == true).forEach((mfile) -> {
            mfile.setSelected(false);
        });
        selectedMediaFile = ((MediaGridCellSR) t.getSource()).getItem();
        searchTools.getInfoBox().setVisible(true);
        if (selectedMediaFile.getRecordTime() != null) {
            searchTools.getMediaFileInfoLabel().setText(selectedMediaFile.getPathStorage().toString()+"\t"+selectedMediaFile.getName() + "\t" + selectedMediaFile.getRecordTime());
        } else {
            try {
                searchTools.getMediaFileInfoLabel().setText(selectedMediaFile.getPathStorage().toString()+"\t"+selectedMediaFile.getName() + "\t" + selectedMediaFile.getCreationTime());
            } catch (IOException ex) {
                Logger.getLogger(MediaGridCellSearchFactory.class.getName()).log(Level.SEVERE, "Cannot read creation time/date from file "+ex.getMessage(), ex);
            }
        }
        selectionModel.clear();
        selectionModel.add(((MediaGridCellSR) t.getSource()).getItem());
        selectedCell = cell;
        cell.requestLayout();
    }

    private void handleGridCellSelection(MouseEvent t) {

    }

    public MediaFile getSelectedMediaFile() {
        return selectedMediaFile;
    }

    public boolean isCellVisible(MediaGridCellSR input) {
        VirtualFlow vf = (VirtualFlow) searchTools.getImageGrid().getChildrenUnmodifiable().get(0);
        boolean ret = false;
        if (vf.getFirstVisibleCell() == null) {
            return false;
        }
        int start = vf.getFirstVisibleCell().getIndex();
        int end = vf.getLastVisibleCell().getIndex();
        if (start == end) {
            return true;
        }
        for (int i = start; i <= end; i++) {
            if (vf.getCell(i).getChildrenUnmodifiable().contains(input)) {
                return true;
            }
        }
        return ret;
    }
    
    public void shutdown(){
        fileLoader.shutdown();
    }

}
