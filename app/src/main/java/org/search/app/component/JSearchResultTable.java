package org.search.app.component;

import org.search.app.model.SearchResultTableModel;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;

public class JSearchResultTable extends JTable {

    public JSearchResultTable(SearchResultTableModel tableModel) {
        super(tableModel);
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component component = super.prepareRenderer(renderer, row, column);
        int rendererWidth = component.getPreferredSize().width;
        TableColumn tableColumn = getColumnModel().getColumn(column);
        tableColumn.setPreferredWidth(Math.max(rendererWidth + getIntercellSpacing().width, tableColumn.getPreferredWidth()));
        return component;
    }

    @Override
    public SearchResultTableModel getModel() {
        return (SearchResultTableModel)super.getModel();
    }
}
