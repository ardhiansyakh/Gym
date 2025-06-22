package Main;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.JOptionPane;

public class HargePaket {

    public void HargePaket(){};
    
    public void ShowPrice(Connection conn, JTable tabel) {
        tabel.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {},
            new String [] {"No", "Member", "Price"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; 
            }
        });
        
        DefaultTableModel tb = (DefaultTableModel) tabel.getModel();
        tb.setRowCount(0);
        tabel.getTableHeader().setReorderingAllowed(false); 
        tabel.getTableHeader().setResizingAllowed(false);
        tabel.setRowSelectionAllowed(true);
        tabel.setColumnSelectionAllowed(false);
        tabel.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        String sql = """
            SELECT jenis, harga
            FROM prices
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = pstmt.executeQuery()) {
                int nomor = 1;
                while (rs.next()) {
                    int harga = rs.getInt("harga");
                    String tipe = rs.getString("jenis");

                    tb.addRow(new Object[]{nomor, tipe, harga});
                    nomor++;
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Gagal mengambil data member: " + e.getMessage());
        }
    }
    
    public void UpdatePrice(Connection conn, JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(null, "Pilih satu baris di tabel terlebih dahulu.");
            return;
        }

        String jenis = table.getValueAt(selectedRow, 1).toString();

        String input = JOptionPane.showInputDialog(null, "Masukkan harga baru untuk " + jenis + ":");
        if (input == null || input.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Input tidak boleh kosong.");
            return;
        }

        int hargaBaru;
        try {
            hargaBaru = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Input harus berupa angka.");
            return;
        }

        String sql = "UPDATE prices SET harga = ? WHERE jenis = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, hargaBaru);
            stmt.setString(2, jenis);
            int hasil = stmt.executeUpdate();
            if (hasil > 0) {
                JOptionPane.showMessageDialog(null, "Harga " + jenis + " berhasil diupdate.");
                ShowPrice(conn, table);
            } else {
                JOptionPane.showMessageDialog(null, "Update gagal. Data mungkin tidak ditemukan.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Gagal update harga: " + e.getMessage());
        }
    }
}
