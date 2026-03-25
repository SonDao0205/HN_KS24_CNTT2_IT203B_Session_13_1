package BTVN.Ex05;

import BTVN.DatabaseConnection;

import java.sql.*;

public class BenhNhanController {
    public boolean tiepNhan1Cham(String ten, int tuoi, String maGiuong, double tien) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Bắt đầu Transaction

            // 1. Thêm bệnh nhân
            String sqlBN = "INSERT INTO BenhNhan (HoTen, Tuoi, MaGiuong) VALUES (?, ?, ?)";
            PreparedStatement psBN = conn.prepareStatement(sqlBN, Statement.RETURN_GENERATED_KEYS);
            psBN.setString(1, ten);
            psBN.setInt(2, tuoi);
            psBN.setString(3, maGiuong);
            psBN.executeUpdate();

            ResultSet rs = psBN.getGeneratedKeys();
            int maBN = rs.next() ? rs.getInt(1) : 0;

            // 2. Cập nhật trạng thái giường
            String sqlGiuong = "UPDATE GiuongBenh SET TrangThai = 1 WHERE MaGiuong = ? AND TrangThai = 0";
            PreparedStatement psGiuong = conn.prepareStatement(sqlGiuong);
            psGiuong.setString(1, maGiuong);
            int updatedGiuong = psGiuong.executeUpdate();

            if (updatedGiuong == 0) throw new SQLException("Giường đã bị người khác chọn hoặc không tồn tại!");

            // 3. Cộng tiền tài chính
            String sqlTien = "INSERT INTO TaiChinh (MaBN, SoTienTamUng, NgayThu) VALUES (?, ?, NOW())";
            PreparedStatement psTien = conn.prepareStatement(sqlTien);
            psTien.setInt(1, maBN);
            psTien.setDouble(2, tien);
            psTien.executeUpdate();

            conn.commit(); // Thành công toàn bộ
            return true;

        } catch (SQLException e) {
            System.err.println("Lỗi hệ thống: " + e.getMessage());
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
}