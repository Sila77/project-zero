package in.project.computers.service.orderService;

import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.dto.order.CreateOrderRequest;
import in.project.computers.dto.order.CreateOrderResponse;
import in.project.computers.dto.order.OrderResponse;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * <h3>OrderService Interface</h3>
 * <p>
 * กำหนดเมธอดมาตรฐาน (สัญญา) สำหรับจัดการ Business Logic ทั้งหมดที่เกี่ยวกับระบบ Order
 * คลาสที่ implement Interface นี้จะต้องมีเมธอดทั้งหมดตามที่ระบุไว้
 * การใช้ Interface ช่วยให้สามารถสลับสับเปลี่ยน implementation ในอนาคตได้ง่าย (Decoupling)
 * และทำให้โครงสร้างของโปรเจกต์มีความชัดเจน
 * </p>
 */
public interface OrderService {

    /**
     * สร้างคำสั่งซื้อ (Order) ใหม่ตามข้อมูลที่ได้รับจากผู้ใช้
     * <p>
     * เมธอดนี้จะรับผิดชอบกระบวนการทั้งหมดตั้งแต่การรวมรายการสินค้า,
     * ตรวจสอบสต็อก, คำนวณยอดรวม, ไปจนถึงการเริ่มต้นกระบวนการชำระเงินตามวิธีที่ผู้ใช้เลือก
     * </p>
     *
     * @param request DTO ที่มีข้อมูลคำสั่งซื้อทั้งหมดจาก Client (เช่น รายการสินค้า, ที่อยู่, วิธีชำระเงิน)
     * @return {@link CreateOrderResponse} ซึ่งเป็น DTO ที่มีข้อมูลสำหรับ Client เพื่อดำเนินการต่อ
     *         เช่น มี Order ID และอาจมีลิงก์สำหรับไปชำระเงินที่ PayPal (`approvalLink`)
     * @throws PayPalRESTException หากเลือกชำระเงินด้วย PayPal แล้วเกิดข้อผิดพลาดในการติดต่อกับ PayPal API
     */
    CreateOrderResponse createOrder(CreateOrderRequest request) throws PayPalRESTException;

    /**
     * ยืนยันและประมวลผลการชำระเงินผ่าน PayPal หลังจากที่ผู้ใช้กดยินยอมในหน้าเว็บของ PayPal
     * <p>
     * เมธอดนี้จะถูกเรียกโดย Endpoint ที่เป็น Callback URL จาก PayPal
     * หน้าที่หลักคือการยืนยันการจ่ายเงิน, ตัดสต็อกสินค้า, และอัปเดตสถานะของ Order
     * </p>
     *
     * @param orderId   ID ของ Order ในระบบของเรา ซึ่งถูกส่งไป-กลับกับ PayPal
     * @param paymentId ID ของ Payment ที่สร้างโดย PayPal (ได้มาจาก Query Parameter)
     * @param payerId   ID ของผู้ชำระเงินที่ระบุโดย PayPal (ได้มาจาก Query Parameter)
     * @return {@link OrderResponse} ที่มีสถานะของ Order ที่อัปเดตแล้ว (เช่น จ่ายเงินสำเร็จแล้ว)
     * @throws PayPalRESTException หากเกิดข้อผิดพลาดในการยืนยันการชำระเงินกับ PayPal API
     */
    OrderResponse capturePaypalOrder(String orderId, String paymentId, String payerId) throws PayPalRESTException;

    /**
     * รับไฟล์สลิปโอนเงินจากผู้ใช้สำหรับ Order ที่เลือกชำระเงินแบบ Bank Transfer
     * <p>
     * เมธอดนี้จะทำการอัปโหลดไฟล์รูปภาพสลิปไปยังบริการจัดเก็บไฟล์ (เช่น AWS S3)
     * และอัปเดตสถานะของ Order เป็น "รอการตรวจสอบ" (Pending Approval)
     * </p>
     *
     * @param orderId   ID ของ Order ที่ต้องการแจ้งชำระเงิน
     * @param slipImage ไฟล์รูปภาพสลิปที่ผู้ใช้อัปโหลด
     * @return {@link OrderResponse} ที่มีสถานะของ Order ที่อัปเดตแล้ว
     */
    OrderResponse submitPaymentSlip(String orderId, MultipartFile slipImage);

    /**
     * ดึงข้อมูล Order ตาม ID ที่ระบุ
     * <p>
     * เมธอดนี้จะมีการตรวจสอบสิทธิ์เพื่อให้แน่ใจว่าผู้ที่ร้องขอเป็นเจ้าของ Order นั้นจริง
     * </p>
     *
     * @param orderId ID ของ Order ที่ต้องการดูข้อมูล
     * @return {@link OrderResponse} ที่มีรายละเอียดทั้งหมดของ Order นั้น
     */
    OrderResponse getOrderById(String orderId);

    /**
     * ดึงรายการ Order ทั้งหมดของผู้ใช้ที่กำลังล็อกอินอยู่ในปัจจุบัน
     * <p>
     * ผลลัพธ์จะถูกเรียงลำดับตามวันที่สร้างล่าสุด (ใหม่สุดอยู่บนสุด)
     * </p>
     *
     * @return {@code List<OrderResponse>} รายการ Order ทั้งหมดของผู้ใช้
     */
    List<OrderResponse> getCurrentUserOrders();

    /**
     * [สำหรับ User] ยกเลิก Order ที่ยังไม่ได้ชำระเงิน หรืออยู่ในสถานะที่อนุญาตให้ยกเลิกได้
     *
     * @param orderId ID ของ Order ที่ต้องการยกเลิก
     * @return {@link OrderResponse} ที่มีสถานะ Order อัปเดตเป็น CANCELLED
     * @throws ResponseStatusException หาก Order ไม่สามารถยกเลิกได้ (เช่น จ่ายเงินไปแล้ว)
     */
    OrderResponse cancelOrder(String orderId);

    /**
     * [สำหรับ User] พยายามชำระเงินใหม่สำหรับ Order ที่เลือกชำระด้วย PayPal และยังไม่ได้ชำระเงิน หรือการชำระเงินล้มเหลว
     * ระบบจะสร้างลิงก์สำหรับไปชำระเงินที่ PayPal ใหม่อีกครั้ง
     *
     * @param orderId ID ของ Order ที่ต้องการลองชำระเงินใหม่
     * @return {@link CreateOrderResponse} ที่มีลิงก์สำหรับไปชำระเงินที่ PayPal ใหม่
     * @throws PayPalRESTException หากเกิดข้อผิดพลาดในการสร้างลิงก์กับ PayPal
     * @throws ResponseStatusException หาก Order ไม่ตรงตามเงื่อนไข
     */
    CreateOrderResponse retryPayment(String orderId) throws PayPalRESTException;

    /**
     * [สำหรับ User] ผู้ใช้ส่งคำขอคืนเงินสำหรับ Order ที่ได้ชำระเงินไปแล้ว
     * ระบบจะเปลี่ยนสถานะ Order เป็น REFUND_REQUESTED เพื่อรอการตรวจสอบจาก Admin
     *
     * @param orderId ID ของ Order ที่ต้องการขอคืนเงิน
     * @return {@link OrderResponse} ที่มีสถานะ Order อัปเดตเป็น REFUND_REQUESTED
     * @throws ResponseStatusException หาก Order ไม่ตรงตามเงื่อนไขการขอคืนเงิน
     */
    OrderResponse requestRefund(String orderId);

    /**
     * [สำหรับ Admin] อนุมัติคำขอคืนเงินที่ผู้ใช้ส่งมา
     * หากเป็นการชำระเงินผ่าน PayPal ระบบจะดำเนินการคืนเงินผ่าน PayPal API
     * จากนั้นจะทำการคืนสต็อกสินค้า และอัปเดตสถานะ Order เป็น REFUNDED
     *
     * @param orderId ID ของ Order ที่ Admin ต้องการอนุมัติการคืนเงิน
     * @return {@link OrderResponse} ที่มีสถานะ Order อัปเดตเป็น REFUNDED
     * @throws PayPalRESTException หากเกิดข้อผิดพลาดในการติดต่อ PayPal API เพื่อทำการ Refund
     * @throws ResponseStatusException หาก Order ไม่ตรงตามเงื่อนไขการอนุมัติคืนเงิน
     */
    OrderResponse approveRefund(String orderId) throws PayPalRESTException;

    /**
     * [สำหรับ Admin] ปฏิเสธคำขอคืนเงินที่ผู้ใช้ส่งมา
     * ระบบจะเปลี่ยนสถานะ Order เป็น REFUND_REJECTED
     *
     * @param orderId ID ของ Order ที่ Admin ต้องการปฏิเสธการคืนเงิน
     * @return {@link OrderResponse} ที่มีสถานะ Order อัปเดตเป็น REFUND_REJECTED
     * @throws ResponseStatusException หาก Order ไม่ตรงตามเงื่อนไขการปฏิเสธคืนเงิน
     */
    OrderResponse rejectRefund(String orderId);
}