//1. Phân tích Rủi ro & Bẫy lỗi (Edge Cases)
//Để hệ thống không bị "sập" hoặc sai lệch dữ liệu, cần xử lý 3 kịch bản chính:
//Sai kiểu dữ liệu (Data Type Mismatch): Nhân viên nhập chữ "Năm trăm" vào ô tiền hoặc nhập tuổi là "2x". Nếu không dùng try-catch bọc lấy Scanner.nextLine() và Double.parseDouble(), chương trình sẽ văng ngoại lệ và tắt ngóm ngay lập tức.
//Tranh chấp tài nguyên (Race Condition): Hai nhân viên cùng thấy giường G01 trống và cùng nhấn xác nhận cho 2 bệnh nhân khác nhau. Nếu không kiểm tra trạng thái giường ngay trong Transaction, hệ thống sẽ xếp 2 người vào 1 giường.
//Lỗi kết nối giữa chừng (Connection Loss): Sau khi INSERT bệnh nhân thành công nhưng mạng lag khiến việc UPDATE trạng thái giường thất bại. Nếu không có Rollback, bệnh nhân sẽ mất tiền tạm ứng nhưng trên hệ thống vẫn không có giường.
//2. Thiết kế Kiến trúc & Cấu trúc Database
//Sơ đồ luồng dữ liệu (Flowchart) cho chức năng Tiếp nhận
//Nhập liệu: Lấy thông tin BN + Mã giường + Tiền.
//Mở Giao dịch: Kết nối DB -> setAutoCommit(false).
//Bước 1: Thêm mới BN vào bảng Patients.
//Bước 2: Cập nhật trạng thái giường trong bảng Beds (Chỉ update nếu status = 'Trống').
//Bước 3: Ghi nhận phiếu thu vào bảng Financials.
//Kết thúc: Nếu tất cả OK -> Commit. Nếu có bất kỳ lỗi nào -> Rollback.
//Cấu trúc các bảng Database
//Patients: id (PK), name, age, bed_id (FK).
//Beds: id (PK), status (0: Trống, 1: Có người).
//Financials: id (PK), patient_id (FK), amount, created_at.


package BTVN.Ex05;

import java.util.Scanner;

public class LeTanView {
    private BenhNhanController controller = new BenhNhanController();
    private Scanner sc = new Scanner(System.in);

    public void displayMenu() {
        while (true) {
            System.out.println("\n--- HỆ THỐNG TIẾP NHẬN 1 CHẠM RIKKEI HOSPITAL ---");
            System.out.println("1. Xem danh sách giường trống");
            System.out.println("2. Tiếp nhận bệnh nhân mới (Transaction)");
            System.out.println("3. Thoát");
            System.out.print("Chọn chức năng: ");

            String choice = sc.nextLine();
            switch (choice) {
                case "1": xemGiuongTrong(); break;
                case "2": xuLyTiepNhan(); break;
                case "3": System.exit(0);
                default: System.out.println("Lựa chọn không hợp lệ!");
            }
        }
    }

    private void xuLyTiepNhan() {
        try {
            System.out.print("Nhập tên bệnh nhân: "); String ten = sc.nextLine();
            System.out.print("Nhập tuổi: "); int tuoi = Integer.parseInt(sc.nextLine());
            System.out.print("Mã giường chọn: "); String maGiuong = sc.nextLine();
            System.out.print("Số tiền tạm ứng: "); double tien = Double.parseDouble(sc.nextLine());

            if (tien < 0) {
                System.out.println("Tiền không thể âm!");
                return;
            }

            if (controller.tiepNhan1Cham(ten, tuoi, maGiuong, tien)) {
                System.out.println(">>> TIẾP NHẬN THÀNH CÔNG! Dữ liệu đã được đồng bộ.");
            } else {
                System.out.println(">>> THẤT BẠI: Đã Rollback dữ liệu. Vui lòng thử lại.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Lỗi: Tuổi và Số tiền phải là con số!");
        }
    }

    private void xemGiuongTrong() {
        System.out.println("Đang truy vấn danh sách giường trống...");
        // Gọi Controller để SELECT * FROM GiuongBenh WHERE TrangThai = 0
    }

    public static void main(String[] args) {
        new LeTanView().displayMenu();
    }
}