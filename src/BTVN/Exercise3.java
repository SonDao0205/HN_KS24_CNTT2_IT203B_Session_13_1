package BTVN;

import java.sql.*;

public class Exercise3 {
    public void xuatVienVaThanhToan(int maBenhNhan, double tienVienPhi) throws Exception {
        Connection conn = null;
        PreparedStatement psBalance = null;
        PreparedStatement psUpdateWallet = null;
        PreparedStatement psUpdateBed = null;
        PreparedStatement psUpdatePatient = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // --- BẪY 1: KIỂM TRA LOGIC NGHIỆP VỤ (THIẾU TIỀN) ---
            String sqlCheckBalance = "SELECT balance FROM Patient_Wallet WHERE patient_id = ?";
            psBalance = conn.prepareStatement(sqlCheckBalance);
            psBalance.setInt(1, maBenhNhan);
            ResultSet rs = psBalance.executeQuery();

            if (rs.next()) {
                double soDuHienTai = rs.getDouble("balance");
                if (soDuHienTai < tienVienPhi) {
                    // Chủ động ném lỗi nếu không đủ tiền, Transaction sẽ nhảy xuống catch để Rollback
                    throw new Exception("LỖI : Số dư tạm ứng không đủ để thanh toán viện phí!");
                }
            } else {
                throw new Exception("LỖI: Không tìm thấy ví tiền của bệnh nhân mã số " + maBenhNhan);
            }

            // --- BƯỚC 1: TRỪ TIỀN VIỆN PHÍ ---
            String sql1 = "UPDATE Patient_Wallet SET balance = balance - ? WHERE patient_id = ?";
            psUpdateWallet = conn.prepareStatement(sql1);
            psUpdateWallet.setDouble(1, tienVienPhi);
            psUpdateWallet.setInt(2, maBenhNhan);
            psUpdateWallet.executeUpdate();

            // --- BƯỚC 2: GIẢI PHÓNG GIƯỜNG BỆNH ---
            // Giả sử bảng Patients có cột bed_id để biết bệnh nhân đang nằm giường nào
            String sql2 = "UPDATE Beds SET status = 'EMPTY' WHERE bed_id = (SELECT bed_id FROM Patients WHERE patient_id = ?)";
            psUpdateBed = conn.prepareStatement(sql2);
            psUpdateBed.setInt(1, maBenhNhan);
            int rowsBed = psUpdateBed.executeUpdate();

            // BẪY 2: Kiểm tra Row Affected cho giường bệnh
            if (rowsBed == 0) {
                throw new Exception("LỖI : Không thể giải phóng giường. Có thể bệnh nhân chưa được gán giường!");
            }

            // --- BƯỚC 3: CẬP NHẬT TRẠNG THÁI BỆNH NHÂN ---
            String sql3 = "UPDATE Patients SET status = 'DISCHARGED' WHERE patient_id = ?";
            psUpdatePatient = conn.prepareStatement(sql3);
            psUpdatePatient.setInt(1, maBenhNhan);
            int rowsPatient = psUpdatePatient.executeUpdate();

            // BẪY 2: Kiểm tra Row Affected cho bệnh nhân
            if (rowsPatient == 0) {
                // Nếu ID không tồn tại, executeUpdate trả về 0 chứ không ném SQLException
                throw new Exception("LỖI : Mã bệnh nhân " + maBenhNhan + " không tồn tại trên hệ thống!");
            }

            // HOÀN TẤT: Nếu chạy đến đây mà không gặp lỗi, tiến hành lưu vĩnh viễn
            conn.commit();
            System.out.println("THÀNH CÔNG: Bệnh nhân " + maBenhNhan + " đã hoàn tất thủ tục xuất viện.");

        } catch (Exception e) {
            // XỬ LÝ SỰ CỐ: Hủy bỏ toàn bộ thao tác nếu có bất kỳ Exception nào
            if (conn != null) {
                try {
                    System.err.println("ĐANG ROLLBACK DỮ LIỆU");
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e; // Ném tiếp lỗi ra ngoài để UI/Controller xử lý thông báo cho người dùng
        } finally {
            // GIẢI PHÓNG TÀI NGUYÊN: Luôn đóng kết nối dù thành công hay thất bại
            try {
                if (psBalance != null) psBalance.close();
                if (psUpdateWallet != null) psUpdateWallet.close();
                if (psUpdateBed != null) psUpdateBed.close();
                if (psUpdatePatient != null) psUpdatePatient.close();
                if (conn != null) {
                    conn.setAutoCommit(true); // Trả lại trạng thái mặc định cho Connection Pool
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
