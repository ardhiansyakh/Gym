package Main;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

//interface
interface UserService {
    User login(String username, String password);
    boolean register(String nama, String password, String role, String paket);
}

//superclass

/**
 *
 * @author Ardhiansyakh
 */
public class User implements UserService { 

    /**
     *
     */
    public int id;

    /**
     *
     */
    public String nama;

    /**
     *
     */
    public String role;

    // overloading

    /**
     *
     */
    public User() {}

    /**
     *
     * @param id
     * @param nama
     * @param role
     */
    public User(int id, String nama, String role) {
        this.id = id;
        this.nama = nama;
        this.role = role;
    }

    // overide

    /**
     *
     * @param username
     * @param password
     * @return
     */
    @Override
    public User login(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                String nama = rs.getString("username");
                String role = rs.getString("role");

                User user;
                if ("admin".equalsIgnoreCase(role)) {
                    user = new Admin(id, nama, role);
                } else {
                    user = new Member(id, nama, role);
                }

                // innstance off
                if (user instanceof Admin) {
                    System.out.println("Ini admin.");
                } else if (user instanceof Member) {
                    System.out.println("Ini member.");
                }

                return user;
            } else {
                JOptionPane.showMessageDialog(null, "Username atau password salah!", "Login Gagal", JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Terjadi kesalahan database:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    /**
     *
     * @param nama
     * @param password
     * @param role
     * @param paket
     * @return
     */
    @Override
    public boolean register(String nama, String password, String role, String paket) {
        if (nama.isEmpty() || password.isEmpty() || role.isEmpty() || paket.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Semua field harus diisi!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        String checkQuery = "SELECT * FROM users WHERE username = ?";
        String insertQuery = "INSERT INTO users (username, password, role, paket, tanggal_pembuatan, waktu_habis) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {

            checkStmt.setString(1, nama);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                JOptionPane.showMessageDialog(null, "Username sudah digunakan, silakan pilih nama lain.", "Gagal", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            java.time.LocalDate sekarang = java.time.LocalDate.now();
            java.time.LocalDate habis;
            if (paket.equalsIgnoreCase("mingguan")) {
                habis = sekarang.plusWeeks(1);
            } else {
                habis = sekarang.plusMonths(1);
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                insertStmt.setString(1, nama);
                insertStmt.setString(2, password);
                insertStmt.setString(3, role);
                insertStmt.setString(4, paket);
                insertStmt.setDate(5, java.sql.Date.valueOf(sekarang)); 
                insertStmt.setDate(6, java.sql.Date.valueOf(habis));   

                int result = insertStmt.executeUpdate();
                if (result > 0) {
                    JOptionPane.showMessageDialog(null, "Registrasi berhasil dengan paket " + paket + "!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                    return true;
                } else {
                    JOptionPane.showMessageDialog(null, "Registrasi gagal. Silakan coba lagi.", "Gagal", JOptionPane.ERROR_MESSAGE);
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Terjadi kesalahan database:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        return false;
    }
    
    //enkapsulasi

    /**
     *
     * @param userId
     * @return
     */
    public String getTipeMemberById(int userId) {
        String sql = "SELECT paket FROM users WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("paket");
            }
            return "Tidak ditemukan";

        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        }
    }
   
    /**
     *
     * @param userId
     * @return
     */
    public String getStatusMasaAktifDanHabisById(int userId) {
        String query = "SELECT waktu_habis FROM users WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Date tanggalHabis = rs.getDate("waktu_habis");

                return tanggalHabis.toString();
            } else {
                return "Data tidak ditemukan";
            }

        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     *
     * @param username
     * @param paketBaru
     */
    public void perpanjangPaket(String username, String paketBaru) {
        String querySelect = "SELECT id, paket, waktu_habis FROM users WHERE username = ?";
        String queryUpdate = "UPDATE users SET paket = ?, waktu_habis = ? WHERE username = ?";
        String queryInsertLog = """
            INSERT INTO log_perpanjangan (user_id, paket_lama, paket_baru, waktu_perpanjang, menjadi_tanggal)
            VALUES (?, ?, ?, NOW(), ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmtSelect = conn.prepareStatement(querySelect)) {

            stmtSelect.setString(1, username);
            ResultSet rs = stmtSelect.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("id");
                String paketLama = rs.getString("paket");
                Timestamp waktuHabisSaatIni = rs.getTimestamp("waktu_habis");
                Timestamp waktuSekarang = new Timestamp(System.currentTimeMillis());

                // Hitung waktu habis baru
                long millisTambahan = paketBaru.equalsIgnoreCase("mingguan") ?
                                      7L * 24 * 60 * 60 * 1000 : // 7 hari
                                      30L * 24 * 60 * 60 * 1000; // 30 hari

                Timestamp waktuBaru = (waktuHabisSaatIni != null && waktuHabisSaatIni.after(waktuSekarang))
                                      ? new Timestamp(waktuHabisSaatIni.getTime() + millisTambahan)
                                      : new Timestamp(waktuSekarang.getTime() + millisTambahan);

                // Update users
                try (PreparedStatement stmtUpdate = conn.prepareStatement(queryUpdate)) {
                    stmtUpdate.setString(1, paketBaru);
                    stmtUpdate.setTimestamp(2, waktuBaru);
                    stmtUpdate.setString(3, username);
                    stmtUpdate.executeUpdate();
                }

                // Masukkan ke log
                try (PreparedStatement stmtLog = conn.prepareStatement(queryInsertLog)) {
                    stmtLog.setInt(1, userId);
                    stmtLog.setString(2, paketLama);
                    stmtLog.setString(3, paketBaru);
                    stmtLog.setTimestamp(4, waktuBaru);
                    stmtLog.executeUpdate();
                }

                JOptionPane.showMessageDialog(null, "Perpanjangan berhasil! Paket aktif hingga: " + waktuBaru);

            } else {
                JOptionPane.showMessageDialog(null, "User tidak ditemukan!");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Gagal memperpanjang paket: " + e.getMessage());
        }
    }

    /**
     *
     * @param conn
     * @param table
     */
    public void ShowUsers(Connection conn, JTable table) {
        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {},
            new String [] {"ID", "Username", "Role", "Paket", "Tanggal Pembuatan", "Waktu Habis"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });

        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        table.getTableHeader().setReorderingAllowed(false);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        String sql = "SELECT id, username, role, paket, tanggal_pembuatan, waktu_habis FROM users";

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String username = rs.getString("username");
                String role = rs.getString("role");
                String paket = rs.getString("paket");
                String tanggal = rs.getString("tanggal_pembuatan");
                String waktuHabis = rs.getString("waktu_habis");

                model.addRow(new Object[]{id, username, role, paket, tanggal, waktuHabis});
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Gagal menampilkan data user: " + e.getMessage());
        }
    }

    /**
     *
     * @param conn
     * @param table
     */
    public void UpdateSelectedUserWithDialog(Connection conn, JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(null, "Silakan pilih user yang ingin diupdate.");
            return;
        }

        int userId = (int) table.getValueAt(selectedRow, 0); 
        String currentRole = (String) table.getValueAt(selectedRow, 2); 
        String currentPaket = (String) table.getValueAt(selectedRow, 3);

        String[] roleOptions = {"admin", "member"};
        String[] paketOptions = {"mingguan", "bulanan"};

        String newRole = (String) JOptionPane.showInputDialog(
                null,
                "Pilih Role Baru:",
                "Update Role",
                JOptionPane.QUESTION_MESSAGE,
                null,
                roleOptions,
                currentRole
        );

        if (newRole == null) return; 

        String newPaket = (String) JOptionPane.showInputDialog(
                null,
                "Pilih Paket Baru:",
                "Update Paket",
                JOptionPane.QUESTION_MESSAGE,
                null,
                paketOptions,
                currentPaket
        );

        if (newPaket == null) return; 

        String query = "UPDATE users SET role = ?, paket = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, newRole);
            stmt.setString(2, newPaket);
            stmt.setInt(3, userId);

            int result = stmt.executeUpdate();
            if (result > 0) {
                JOptionPane.showMessageDialog(null, "User berhasil diperbarui!");
                ShowUsers(conn, table);
            } else {
                JOptionPane.showMessageDialog(null, "Gagal memperbarui user.");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Kesalahan database:\n" + e.getMessage());
        }
    }

    /**
     *
     * @param conn
     * @param table
     */
    public void deleteSelectedUser(Connection conn, JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(null, "Silakan pilih user yang ingin dihapus.");
            return;
        }

        int userId = (int) table.getValueAt(selectedRow, 0);

        int confirm = JOptionPane.showConfirmDialog(
            null,
            "Apakah Anda yakin ingin menghapus user dengan ID " + userId + "?",
            "Konfirmasi Hapus",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String query = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            int result = stmt.executeUpdate();
            if (result > 0) {
                JOptionPane.showMessageDialog(null, "User berhasil dihapus.");
            } else {
                JOptionPane.showMessageDialog(null, "Gagal menghapus user.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Kesalahan database:\n" + e.getMessage());
        }
    }
    
    /**
     *
     * @param conn
     * @param table
     * @param username
     */
    public void ShowLogPerpanjangan(Connection conn, JTable table, String username) {
        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object[][] {},
            new String[]{"Username", "Paket Lama", "Paket Baru", "Tanggal Update", "Menjadi"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });

        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        String sql = """
            SELECT u.username, l.paket_lama, l.paket_baru, l.waktu_perpanjang, l.menjadi_tanggal
            FROM log_perpanjangan l
            JOIN users u ON u.id = l.user_id
            WHERE u.username = ?
            ORDER BY l.waktu_perpanjang DESC
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String uname = rs.getString("username");
                String paketLama = rs.getString("paket_lama");
                String paketBaru = rs.getString("paket_baru");
                String waktuPerpanjang = rs.getString("waktu_perpanjang");
                String menjadiTanggal = rs.getString("menjadi_tanggal");

                model.addRow(new Object[]{uname, paketLama, paketBaru, waktuPerpanjang, menjadiTanggal});
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Gagal menampilkan log perpanjangan: " + e.getMessage());
        }
    }
}

// subclass
class Admin extends User {
    public Admin(int id, String nama, String role) {
        super(id, nama, role);
    }
}

//subclass
class Member extends User {
    public Member(int id, String nama, String role) {
        super(id, nama, role);
    }
}