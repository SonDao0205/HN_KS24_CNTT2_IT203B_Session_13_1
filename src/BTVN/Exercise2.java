// phân tích : Vi phạm tính nhất quán (Consistency): Khi setAutoCommit(false), các thay đổi ở Bước 1 (Trừ tiền) thực chất đã được gửi xuống Database và nằm trong trạng thái "Pending" (chờ xử lý). Nếu không có lệnh hủy bỏ, các dòng dữ liệu bị tác động (ví dụ: số dư ví của bệnh nhân) có thể bị Database Lock (khóa). Các tiến trình khác muốn truy cập vào dòng dữ liệu này sẽ phải chờ đợi vô thời hạn, dẫn đến tình trạng treo hệ thống
// trong code đã quên lệnh conn.rollback(). Trong một Transaction, nếu một mắt xích thất bại, chúng ta phải phát đi tín hiệu tường minh để Database hủy bỏ toàn bộ các thay đổi tạm thời và giải phóng các tài nguyên (locks) đang nắm giữ.

package BTVN;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Exercise2 {
    public void thanhToanVienPhi(int patientId, int invoiceId, double amount) {
        Connection conn = null;
        try{
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            String sqlDeductWallet = "UPDATE Patient_Wallet SET balance = balance - ? WHERE patient_id = ?";
            try (PreparedStatement ps1 = conn.prepareStatement(sqlDeductWallet)) {
                ps1.setDouble(1, amount);
                ps1.setInt(2, patientId);
                ps1.executeUpdate();
            }

            String sqlUpdateInvoice = "UPDATE Invoicesss SET status = 'PAID' WHERE invoice_id = ?";
            try (PreparedStatement ps2 = conn.prepareStatement(sqlUpdateInvoice)) {
                ps2.setInt(1, invoiceId);
                ps2.executeUpdate();
            }

            conn.commit();
            System.out.println("Thanh toán hoàn tất!");

        } catch (SQLException e) {
            System.out.println("Lỗi : Không thể hoàn tất thanh toán. Chi tiết: " + e.getMessage());
            if (conn != null) {
                try {
                    System.out.println("khôi phục dữ liệu");
                    conn.rollback();
                    System.out.println("Khôi phục dữ liệu thành công");
                } catch (SQLException rollbackEx) {
                    System.err.println("Lỗi Không thể rollback! " + rollbackEx.getMessage());
                }
            }
        }
    }
}
