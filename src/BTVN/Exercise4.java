//1. Phân tích & Đề xuất Giải pháp
//Dữ liệu đầu vào (Input) & Đầu ra (Output)
//Input: ngayHienTai (Date) hoặc maKhoa (int).
//Output: List<BenhNhanDTO> (Mỗi DTO chứa thông tin bệnh nhân và List<DichVu>).
//Đề xuất 2 giải pháp JDBC
//Giải pháp 1 (N+1 Query - Sai lầm cũ): * Query 1: Lấy danh sách 500 bệnh nhân.
//Vòng lặp (500 lần): Với mỗi bệnh nhân, bắn thêm 1 Query vào DB để lấy danh sách dịch vụ.
//Tổng cộng: 501 Queries.
//Giải pháp 2 (Join & Map - Tối ưu):
//Sử dụng duy nhất 1 Query dùng LEFT JOIN để lấy tất cả bệnh nhân và dịch vụ đi kèm trong một lần truy vấn duy nhất.
//Dùng Map<Integer, BenhNhanDTO> trong Java để gộp dữ liệu trùng lặp.
//2. So sánh & Lựa chọn
//Tiêu chí          Giải pháp 1 (N+1)                           Giải pháp 2 (JOIN & MAP)
//Số lượng          Query,Rất lớn (N+1)                         Duy nhất 1
//Network I/O       Quá tải do đóng/mở connection liên tục      "Cực thấp, tối ưu đường truyền"
//Tốc độ xử lý      10 - 15 giây (Rất chậm)                     < 1 giây (Rất nhanh)
//Độ phức tạp       Đơn giản                                    Phức tạp hơn (cần xử lý Map)
//Chốt lựa chọn: Giải pháp 2 là tối ưu nhất vì triệt tiêu độ trễ mạng và đáp ứng yêu cầu < 1s của Giám đốc bệnh viện.

//3. Thiết kế & Triển khai
//Thiết kế câu lệnh SQL & Logic xử lý
//SQL: SELECT bn.*, dv.* FROM BenhNhan bn LEFT JOIN DichVu dv ON bn.id = dv.bn_id.
//Lưu ý: Dùng LEFT JOIN thay vì INNER JOIN để tránh Bẫy 2 (mất bệnh nhân chưa có dịch vụ).
//Java Logic: * Tạo một Map<Integer, BenhNhanDTO>.
//Duyệt qua ResultSet. Nếu maBN chưa có trong Map -> Tạo mới DTO.
//Nếu dòng hiện tại có thông tin dịch vụ (không null) -> Thêm dịch vụ vào danh sách của DTO tương ứng.
package BTVN;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.sql.Date;

class BenhNhanDTO{
    int id;
    String name;
    int age;
    List<DichVu> dsDichVu;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public List<DichVu> getDsDichVu() {
        return dsDichVu;
    }

    public void setDsDichVu(List<DichVu> dsDichVu) {
        this.dsDichVu = dsDichVu;
    }
}

class DichVu{
    int id;
    String name;
    double price;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}

public class Exercise4 {
    public List<BenhNhanDTO> getDashboardData(Date ngay) {
        // Sử dụng LinkedHashMap để đảm bảo thứ tự bệnh nhân không bị đảo lộn
        Map<Integer, BenhNhanDTO> mapBenhNhan = new LinkedHashMap<>();

        // Câu lệnh LEFT JOIN giải quyết triệt để Bẫy 2 (không mất bệnh nhân mới)
        String sql = "SELECT b.id, b.ten_benh_nhan, b.tuoi, " +
                "d.id AS dv_id, d.ten_dich_vu, d.gia " +
                "FROM BenhNhan b " +
                "LEFT JOIN DichVuSuDung d ON b.id = d.ma_benh_nhan " +
                "WHERE b.ngay_truc = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, ngay);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int patientId = rs.getInt("id");

                // Bước 1: Kiểm tra xem bệnh nhân đã tồn tại trong Map chưa
                BenhNhanDTO dto = mapBenhNhan.get(patientId);

                if (dto == null) {
                    dto = new BenhNhanDTO();
                    dto.setId(patientId);
                    dto.setName(rs.getString("ten_benh_nhan"));

                    dto.setAge(rs.getInt("tuoi"));
                    dto.setDsDichVu(new ArrayList<>()); // Khởi tạo List trống tránh NullPointerException

                    mapBenhNhan.put(patientId, dto);
                }

                // Bẫy 2: Xử lý dữ liệu dịch vụ (Tránh thêm dịch vụ NULL vào List)
                int dichVuId = rs.getInt("dv_id");
                if (!rs.wasNull()) { // Kiểm tra xem cột ID dịch vụ có thực sự có dữ liệu không
                    DichVu dv = new DichVu();
                    dv.setId(dichVuId);
                    dv.setName(rs.getString("ten_dich_vu"));
                    dv.setPrice(rs.getDouble("gia"));

                    dto.getDsDichVu().add(dv);
                }
                // Nếu rs.wasNull() là true (Bệnh nhân mới chưa có dịch vụ),
                // chúng ta bỏ qua việc add dịch vụ, dto vẫn giữ dsDichVu rỗng -> An toàn!
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new ArrayList<>(mapBenhNhan.values());
    }
}
