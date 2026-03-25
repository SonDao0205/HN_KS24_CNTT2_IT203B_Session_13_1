package BTVN.BTTHTH;

import BTVN.DatabaseConnection;

import java.sql.*;

public class ThanhToanService {

    public void xuLyXuatVien(int maBN, double soTien, boolean simulateError) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            System.out.println("--- Bắt đầu quy trình xuất viện cho BN: " + maBN + " ---");

            // 1. Lập hóa đơn
            String sqlInvoice = "INSERT INTO INVOICES (MaBN, SoTien, NgayLap) VALUES (?, ?, NOW())";
            PreparedStatement ps1 = conn.prepareStatement(sqlInvoice);
            ps1.setInt(1, maBN);
            ps1.setDouble(2, soTien);
            ps1.executeUpdate();
            System.out.println("[1/3] Đã lập hóa đơn thành công.");

            // 2. Cập nhật trạng thái bệnh nhân
            String sqlPatient = "UPDATE PATIENTS SET TrangThai = 'Đã xuất viện' WHERE MaBN = ?";
            PreparedStatement ps2 = conn.prepareStatement(sqlPatient);
            ps2.setInt(1, maBN);
            ps2.executeUpdate();
            System.out.println("[2/3] Đã cập nhật trạng thái Bệnh nhân.");

            // 3. Giải phóng giường bệnh (Có kịch bản giả lập lỗi)
            String tableName = simulateError ? "BEDZZZ" : "BEDS"; // Giả lập lỗi cú pháp SQL
            String sqlBed = "UPDATE " + tableName + " SET TrangThai = 'Trống', MaBN = NULL WHERE MaBN = ?";
            PreparedStatement ps3 = conn.prepareStatement(sqlBed);
            ps3.setInt(1, maBN);
            ps3.executeUpdate();
            System.out.println("[3/3] Đã giải phóng giường bệnh.");

            // Nếu chạy đến đây không có lỗi -> CHỐT DỮ LIỆU
            conn.commit();
            System.out.println(">>> THÀNH CÔNG: Bệnh nhân " + maBN + " đã hoàn tất thủ tục!");

        } catch (SQLException e) {
            System.err.println(">>> LỖI HỆ THỐNG: " + e.getMessage());
            if (conn != null) {
                try {
                    System.err.println(">>> ĐANG ROLLBACK... Toàn bộ dữ liệu sẽ được khôi phục trạng thái cũ.");
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
}