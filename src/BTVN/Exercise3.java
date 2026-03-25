//1. Phân tích Bài toán (I/O)
//Việc xác định rõ dữ liệu giúp hệ thống kiểm soát chặt chẽ các kịch bản lỗi ngay từ đầu vào.
//Dữ liệu đầu vào (Input):
//maBenhNhan (int): ID duy nhất của bệnh nhân. Dùng để định vị ví tiền, giường bệnh và hồ sơ.
//tienVienPhi (double): Số tiền cần thanh toán. Phải là số dương để tránh lỗi logic nghiệp vụ.
//Kết quả trả về (Output):
//Thành công: Thông báo "Xuất viện thành công", Database cập nhật đồng bộ 3 bảng (Tiền trừ, Giường trống, Trạng thái đã xuất viện).
//Thất bại: Ném ra Exception kèm thông điệp cụ thể (Ví dụ: "Số dư không đủ" hoặc "Mã bệnh nhân không tồn tại"). Mọi thay đổi trung gian đều bị hủy bỏ (Rollback).
//2. Đề xuất Giải pháp kỹ thuật
//Để giải quyết nguyên lý All-or-Nothing, chúng ta sử dụng cơ chế kiểm soát giao dịch thủ công của JDBC:
//Cơ chế chính: Sử dụng connection.setAutoCommit(false). Dữ liệu sẽ chỉ được ghi xuống đĩa khi lệnh commit() được gọi ở cuối luồng xử lý thành công.
//Giải quyết Bẫy 1 (Thiếu tiền): Thực hiện một truy vấn SELECT số dư trước khi UPDATE. Nếu số dư thấp hơn viện phí, dùng lệnh throw new Exception() để chủ động ngắt luồng và nhảy vào khối catch để Rollback.
//Giải quyết Bẫy 2 (Dữ liệu ảo): Kiểm tra giá trị trả về của executeUpdate(). Nếu kết quả bằng 0, nghĩa là WHERE maBenhNhan không khớp với dòng nào. Lúc này, ta cũng chủ động ném ngoại lệ để kích hoạt Rollback, tránh việc xác nhận một giao dịch "rỗng".

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
