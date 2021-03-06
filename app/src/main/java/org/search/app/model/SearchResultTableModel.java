package org.search.app.model;


import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class SearchResultTableModel extends AbstractTableModel {

    private List<RowFile> data = new ArrayList<>();
    private String searchQuery;

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }


    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 1:
                return "Row Number";
            default:
                return "Path";
        }
    }

    @Override
    public String getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 1:
                if (rowIndex >= data.size())
                    return "";
                return String.valueOf(data.get(rowIndex).getRowNumber());
            default:
                if (rowIndex >= data.size())
                    return "";
                return data.get(rowIndex).getFilePath();
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public RowFile getRowFile(int rowIndex) {
        return data.get(rowIndex);
    }

    public void setData(List<RowFile> data) {
        this.data = data;
    }

    public List<RowFile> getData() {
        return data;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public String getSearchQuery() {
        return searchQuery;
    }
}
