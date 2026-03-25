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