package BTVN.BTTHTH;

public class Main {
    public static void main(String[] args) {
        ThanhToanService service = new ThanhToanService();

        // TEST CASE 1: Happy Path (Thành công)
         service.xuLyXuatVien(101, 500000, false);

        // TEST CASE 2: Rollback (Thất bại do sai tên bảng)
//        service.xuLyXuatVien(101, 500000, true);

    }
}
