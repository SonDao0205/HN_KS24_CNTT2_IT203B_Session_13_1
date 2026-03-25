// phân tích : Lý do khiến thuốc trong kho vẫn bị trừ mặc dù có lỗi xảy ra ở giữa là do cơ chế Auto-Commit mặc định của JDBC.
// Do lệnh đầu tiên đã được commit "riêng lẻ", nên dù lệnh thứ hai chưa chạy, dữ liệu ở lệnh một cũng không thể tự động quay lại trạng thái cũ
package BTVN;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Exercise1 {
    public void capPhatThuoc(int medicineId, int patientId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            String sqlUpdateInventory = "UPDATE Medicine_Inventory SET quantity = quantity - 1 WHERE medicine_id = ?";
            PreparedStatement ps1 = conn.prepareStatement(sqlUpdateInventory);
            ps1.setInt(1, medicineId);
            ps1.executeUpdate();

            int x = 10 / 0;

            String sqlInsertHistory = "INSERT INTO Prescription_History (patient_id, medicine_id, date) VALUES (?, ?, GETDATE())";
            PreparedStatement ps2 = conn.prepareStatement(sqlInsertHistory);
            ps2.setInt(1, patientId);
            ps2.setInt(2, medicineId);
            ps2.executeUpdate();

            conn.commit();
            System.out.println("Cấp phát thuốc thành công!");

        } catch (Exception e) {
            try {
                if (conn != null) {
                    conn.rollback();
                    System.out.println("Phát hiện lỗi đã rollback: " + e.getMessage());
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
