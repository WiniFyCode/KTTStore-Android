// Orders.jsx - Trang danh sách đơn hàng của người dùng
import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import axiosInstance from '../../../utils/axios';
import { 
  FaBox, 
  FaTruck, 
  FaCheck, 
  FaTimes, 
  FaSearch, 
  FaShoppingBag, 
  FaMoneyBillWave, 
  FaRegClock,
  FaBoxOpen, 
  FaShippingFast, 
  FaHome,
  FaChevronLeft,
  FaChevronRight,
  FaUser,
  FaMapMarkerAlt
} from 'react-icons/fa';
import PageBanner from '../../../components/PageBanner';

const Orders = () => {
  // Các state quản lý dữ liệu
  const [orders, setOrders] = useState([]); // Danh sách đơn hàng
  const [loading, setLoading] = useState(true); // Trạng thái loading
  const [searchTerm, setSearchTerm] = useState(''); // Từ khóa tìm kiếm
  const [filterStatus, setFilterStatus] = useState('all'); // Bộ lọc trạng thái
  const [sortOrder, setSortOrder] = useState('desc'); // Thứ tự sắp xếp
  const [currentPage, setCurrentPage] = useState(1); // Trang hiện tại
  const [ordersPerPage] = useState(10); // Số đơn hàng mỗi trang
  const [orderStats, setOrderStats] = useState({ // Thống kê đơn hàng
    total: 0, // Tổng số đơn
    pending: 0, // Đơn chờ xác nhận
    processing: 0, // Đơn đang xử lý
    completed: 0, // Đơn hoàn thành
    cancelled: 0, // Đơn đã hủy
    refunded: 0, // Đơn hoàn tiền
    totalSpent: 0, // Tổng chi tiêu
    totalSaved: 0 // Tổng tiết kiệm
  });
  const [theme, setTheme] = useState('tet'); // Theme hiện tại

  // Format giá tiền
  const formatCurrency = (amount) => {
    return amount?.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".");
  };

  // Fetch dữ liệu khi component mount
  useEffect(() => {
    fetchOrders();
  }, []);

  // Tính toán thống kê khi danh sách đơn hàng thay đổi
  useEffect(() => {
    calculateOrderStats();
  }, [orders]);

  // Hàm tính toán thống kê đơn hàng
  const calculateOrderStats = () => {
    const stats = {
      total: orders.length,
      pending: 0,
      processing: 0,
      completed: 0,
      cancelled: 0,
      refunded: 0,
      totalSpent: 0,
      totalSaved: 0
    };

    orders.forEach(order => {
      // Đếm số lượng theo trạng thái
      stats[order.orderStatus]++;
      
      // Tính tổng chi tiêu
      stats.totalSpent += order.paymentPrice;
      
      // Tính tổng tiền tiết kiệm (chênh lệch giữa giá gốc và giá đã giảm)
      stats.totalSaved += (order.totalPrice - order.paymentPrice);
    });

    setOrderStats(stats);
  };

  // Hàm lấy danh sách đơn hàng từ API
  const fetchOrders = async () => {
    try {
      const response = await axiosInstance.get('/api/order/my-orders');
      setOrders(response.data.orders);
    } catch (error) {
      console.error('Lỗi khi lấy danh sách đơn hàng(Orders.jsx):', error);
      toast.error('Không thể tải danh sách đơn hàng');
    } finally {
      setLoading(false);
    }
  };

  // Component hiển thị timeline đơn hàng
  const OrderTimeline = ({ order }) => {
    // Các bước xử lý đơn hàng theo thứ tự
    const steps = [
      { 
        status: 'pending', 
        icon: FaShoppingBag, 
        label: 'Chờ xác nhận',
        description: 'Đơn hàng đang chờ xác nhận từ cửa hàng',
        color: 'from-yellow-500 to-orange-500',
        bgColor: 'bg-yellow-50',
        borderColor: 'border-yellow-200'
      },
      { 
        status: 'confirmed', 
        icon: FaCheck, 
        label: 'Đã xác nhận',
        description: 'Đơn hàng đã được xác nhận và đang chuẩn bị',
        color: 'from-blue-500 to-indigo-500',
        bgColor: 'bg-blue-50',
        borderColor: 'border-blue-200'
      },
      { 
        status: 'shipping', 
        icon: FaShippingFast, 
        label: 'Đang giao hàng',
        description: 'Đơn hàng đang được giao đến bạn',
        color: 'from-indigo-500 to-purple-500',
        bgColor: 'bg-indigo-50',
        borderColor: 'border-indigo-200'
      },
      { 
        status: 'delivered', 
        icon: FaHome, 
        label: 'Đã giao hàng',
        description: 'Đơn hàng đã được giao thành công',
        color: 'from-green-500 to-emerald-500',
        bgColor: 'bg-green-50',
        borderColor: 'border-green-200'
      }
    ];

    // Hàm xác định bước hiện tại của đơn hàng
    const getCurrentStep = () => {
      // Nếu đơn đã hủy hoặc hoàn trả
      if (order.orderStatus === 'cancelled' || order.shippingStatus === 'returned') {
        return -1;
      }

      // Nếu đơn đã hoàn thành
      if (order.orderStatus === 'completed') {
        return 3;
      }

      // Kiểm tra trạng thái vận chuyển
      if (order.shippingStatus === 'shipping') {
        return 2;
      }
      if (order.shippingStatus === 'delivered') {
        return 3;
      }

      // Kiểm tra trạng thái đơn hàng
      if (order.orderStatus === 'confirmed' || order.orderStatus === 'processing') {
        return 1;
      }

      // Mặc định là bước đầu tiên (pending)
      return 0;
    };

    const currentStep = getCurrentStep();

    // Hiển thị thông báo nếu đơn hàng đã hủy hoặc hoàn trả
    if (currentStep === -1) {
      const isCancelled = order.orderStatus === 'cancelled';
      return (
        <div className="py-4">
          <div className="flex items-center justify-center">
            <div className={`px-6 py-3 rounded-xl backdrop-blur-md ${
              isCancelled 
                ? 'bg-red-50/80 text-red-500 border border-red-200' 
                : 'bg-orange-50/80 text-orange-500 border border-orange-200'
            }`}>
              <div className="flex items-center gap-2">
                {isCancelled ? <FaTimes className="w-5 h-5" /> : <FaBoxOpen className="w-5 h-5" />}
                <span className="font-medium">
                  {isCancelled ? 'Đơn hàng đã bị hủy' : 'Đơn hàng đã được hoàn trả'}
                </span>
              </div>
            </div>
          </div>
        </div>
      );
    }

    return (
      <div className="py-8">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {steps.map((step, index) => {
            const isCompleted = index <= currentStep;
            const isCurrent = index === currentStep;
            
            return (
              <div 
                key={step.status}
                className={`relative p-4 rounded-xl backdrop-blur-md border transition-all duration-300 ${
                  isCompleted
                    ? `${step.bgColor} ${step.borderColor}`
                    : 'bg-gray-50/50 border-gray-200'
                } ${isCurrent ? 'ring-2 ring-offset-2 ring-blue-500/50' : ''}`}
              >
                {/* Icon và label */}
                <div className="relative flex items-start gap-4">
                  <div className={`w-12 h-12 rounded-xl flex items-center justify-center transition-all duration-300 ${
                    isCompleted
                      ? `bg-gradient-to-br ${step.color} text-white shadow-lg`
                      : 'bg-gray-100 text-gray-400'
                  }`}>
                    <step.icon className="w-6 h-6" />
                  </div>

                  <div className="flex-1">
                    <h3 className={`font-medium ${
                      isCompleted ? 'text-gray-900' : 'text-gray-500'
                    }`}>
                      {step.label}
                    </h3>
                    
                    {isCurrent && (
                      <p className="text-sm text-gray-500 mt-1">
                        {step.description}
                      </p>
                    )}

                    {/* Hiển thị thời gian cho bước hiện tại */}
                    {isCurrent && order.updatedAt && (
                      <div className="flex items-center gap-1 mt-2 text-xs text-gray-400">
                        <FaRegClock className="w-3 h-3" />
                        <span>
                          {new Date(order.updatedAt).toLocaleTimeString('vi-VN', {
                            hour: '2-digit',
                            minute: '2-digit',
                            day: '2-digit',
                            month: '2-digit'
                          })}
                        </span>
                      </div>
                    )}
                  </div>

                  {/* Check icon cho bước đã hoàn thành */}
                  {isCompleted && !isCurrent && (
                    <div className="absolute top-0 right-0 w-6 h-6 rounded-full bg-green-500 text-white flex items-center justify-center transform translate-x-1/3 -translate-y-1/3">
                      <FaCheck className="w-3 h-3" />
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>

        
      </div>
    );
  };

  // Hàm xử lý hủy đơn hàng
  const handleCancelOrder = async (orderId) => {
    try {
      await axiosInstance.post(`/api/order/cancel/${orderId}`);
      toast.success('Đã hủy đơn hàng thành công');
      fetchOrders(); // Tải lại danh sách sau khi hủy
    } catch (error) {
      console.error('Lỗi khi hủy đơn hàng(Orders.jsx):', error);
      toast.error('Không thể hủy đơn hàng');
    }
  };

  // Hàm lấy thông tin hiển thị theo trạng thái đơn hàng
  const getStatusInfo = (status) => {
    switch (status) {
      case 'pending':
        return {
          icon: <FaBox className="text-yellow-500" />,
          text: 'Chờ xác nhận',
          color: 'text-yellow-500',
          bgColor: 'bg-yellow-50'
        };
      case 'confirmed':
      case 'processing':
        return {
          icon: <FaTruck className="text-blue-500" />,
          text: status === 'confirmed' ? 'Đã xác nhận' : 'Đang xử lý',
          color: 'text-blue-500',
          bgColor: 'bg-blue-50'
        };
      case 'completed':
        return {
          icon: <FaCheck className="text-green-500" />,
          text: 'Hoàn thành',
          color: 'text-green-500',
          bgColor: 'bg-green-50'
        };
      case 'cancelled':
        return {
          icon: <FaTimes className="text-red-500" />,
          text: 'Đã hủy',
          color: 'text-red-500',
          bgColor: 'bg-red-50'
        };
      case 'refunded':
        return {
          icon: <FaTimes className="text-orange-500" />,
          text: 'Đã hoàn tiền',
          color: 'text-orange-500',
          bgColor: 'bg-orange-50'
        };
      default:
        return {
          icon: null,
          text: status,
          color: 'text-gray-500',
          bgColor: 'bg-gray-50'
        };
    }
  };

  // Lọc và sắp xếp danh sách đơn hàng
  const filteredAndSortedOrders = orders
    .filter(order => {
      // Lọc theo trạng thái
      if (filterStatus !== 'all' && order.orderStatus !== filterStatus) {
        return false;
      }
      // Lọc theo từ khóa tìm kiếm
      if (searchTerm) {
        const searchLower = searchTerm.toLowerCase();
        return (
          order.orderID.toString().includes(searchLower) ||
          order.fullname.toLowerCase().includes(searchLower) ||
          order.address.toLowerCase().includes(searchLower)
        );
      }
      return true;
    })
    .sort((a, b) => {
      // Sắp xếp theo thời gian
      const dateA = new Date(a.createdAt);
      const dateB = new Date(b.createdAt);
      return sortOrder === 'asc' ? dateA - dateB : dateB - dateA;
    });

  // Tính toán phân trang
  const indexOfLastOrder = currentPage * ordersPerPage;
  const indexOfFirstOrder = indexOfLastOrder - ordersPerPage;
  const currentOrders = filteredAndSortedOrders.slice(indexOfFirstOrder, indexOfLastOrder);
  const totalPages = Math.ceil(filteredAndSortedOrders.length / ordersPerPage);

  // Thay đổi trang
  const paginate = (pageNumber) => {
    if (pageNumber > 0 && pageNumber <= totalPages) {
      setCurrentPage(pageNumber);
    }
  };

  // Component phân trang
  const Pagination = () => {
    const pageNumbers = [];
    const maxVisiblePages = 5;
    let startPage = Math.max(1, currentPage - Math.floor(maxVisiblePages / 2));
    let endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);

    if (endPage - startPage + 1 < maxVisiblePages) {
      startPage = Math.max(1, endPage - maxVisiblePages + 1);
    }

    for (let i = startPage; i <= endPage; i++) {
      pageNumbers.push(i);
    }

    return (
      <div className="flex justify-center items-center mt-8 gap-2">
        {/* Nút Previous */}
        <button
          onClick={() => paginate(currentPage - 1)}
          disabled={currentPage === 1}
          className={`p-2 rounded-lg ${
            currentPage === 1
              ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
              : theme === 'tet'
                ? 'bg-red-100 text-red-500 hover:bg-red-200'
                : 'bg-blue-100 text-blue-500 hover:bg-blue-200'
          } transition-colors`}
        >
          <FaChevronLeft className="w-5 h-5" />
        </button>

        {/* Số trang */}
        {startPage > 1 && (
          <>
            <button
              onClick={() => paginate(1)}
              className={`w-10 h-10 rounded-lg ${
                theme === 'tet'
                  ? 'hover:bg-red-100 text-gray-700'
                  : 'hover:bg-blue-100 text-gray-700'
              }`}
            >
              1
            </button>
            {startPage > 2 && (
              <span className="px-2 text-gray-500">...</span>
            )}
          </>
        )}

        {pageNumbers.map(number => (
          <button
            key={number}
            onClick={() => paginate(number)}
            className={`w-10 h-10 rounded-lg ${
              currentPage === number
                ? theme === 'tet'
                  ? 'bg-red-500 text-white'
                  : 'bg-blue-500 text-white'
                : theme === 'tet'
                  ? 'hover:bg-red-100 text-gray-700'
                  : 'hover:bg-blue-100 text-gray-700'
            }`}
          >
            {number}
          </button>
        ))}

        {endPage < totalPages && (
          <>
            {endPage < totalPages - 1 && (
              <span className="px-2 text-gray-500">...</span>
            )}
            <button
              onClick={() => paginate(totalPages)}
              className={`w-10 h-10 rounded-lg ${
                theme === 'tet'
                  ? 'hover:bg-red-100 text-gray-700'
                  : 'hover:bg-blue-100 text-gray-700'
              }`}
            >
              {totalPages}
            </button>
          </>
        )}

        {/* Nút Next */}
        <button
          onClick={() => paginate(currentPage + 1)}
          disabled={currentPage === totalPages}
          className={`p-2 rounded-lg ${
            currentPage === totalPages
              ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
              : theme === 'tet'
                ? 'bg-red-100 text-red-500 hover:bg-red-200'
                : 'bg-blue-100 text-blue-500 hover:bg-blue-200'
          } transition-colors`}
        >
          <FaChevronRight className="w-5 h-5" />
        </button>
      </div>
    );
  };

  // Thay thế phần loading return với:
  if (loading) {
    return (
      <div className={`min-h-screen ${
        theme === 'tet' 
          ? 'bg-gradient-to-br from-red-50 via-orange-50 to-yellow-50' 
          : 'bg-gradient-to-br from-blue-50 via-indigo-50 to-purple-50'
      }`}>
        <PageBanner
          theme={theme}
          icon={FaShoppingBag}
          title="Đơn Hàng Của Tôi"
          breadcrumbText="Đơn hàng"
        />

        <div className="max-w-7xl mx-auto px-4 py-8 relative">
          {/* Phần thống kê đơn hàng - Loading */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
            {[...Array(4)].map((_, index) => (
              <div key={index} className="backdrop-blur-md bg-white/30 border border-white/50 rounded-2xl p-6 shadow-xl">
                <div className="flex items-center justify-between">
                  <div className="space-y-2">
                    <div className="w-32 h-4 bg-gray-200 rounded animate-pulse"></div>
                    <div className="w-24 h-8 bg-gray-200 rounded animate-pulse"></div>
                  </div>
                  <div className="w-12 h-12 rounded-xl bg-gray-200 animate-pulse"></div>
                </div>
              </div>
            ))}
          </div>

          {/* Header và công cụ - Loading */}
          <div className="backdrop-blur-md bg-white/50 rounded-2xl p-6 mb-8 border border-white/50 shadow-lg">
            <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
              <div className="w-48 h-8 bg-gray-200 rounded-lg animate-pulse"></div>
              
              <div className="flex flex-col md:flex-row gap-4 w-full md:w-auto">
                <div className="w-full md:w-64 h-10 bg-gray-200 rounded-xl animate-pulse"></div>
                <div className="flex gap-3">
                  <div className="w-40 h-10 bg-gray-200 rounded-xl animate-pulse"></div>
                  <div className="w-32 h-10 bg-gray-200 rounded-xl animate-pulse"></div>
                </div>
              </div>
            </div>
          </div>

          {/* Danh sách đơn hàng - Loading */}
          <div className="grid gap-6">
            {[...Array(3)].map((_, index) => (
              <div key={index} className="bg-white rounded-xl shadow-md">
                {/* Header đơn hàng */}
                <div className="p-6">
                  <div className="flex items-center justify-between mb-6">
                    <div className="flex items-center gap-4">
                      <div className="w-12 h-12 rounded-xl bg-gray-200 animate-pulse"></div>
                      <div>
                        <div className="w-40 h-6 bg-gray-200 rounded animate-pulse mb-2"></div>
                        <div className="w-32 h-4 bg-gray-200 rounded animate-pulse"></div>
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="w-32 h-6 bg-gray-200 rounded animate-pulse mb-2"></div>
                      <div className="w-24 h-4 bg-gray-200 rounded animate-pulse"></div>
                    </div>
                  </div>

                  {/* Timeline Loading */}
                  <div className="py-8">
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                      {[...Array(4)].map((_, stepIndex) => (
                        <div key={stepIndex} className="relative p-4 rounded-xl backdrop-blur-md border border-gray-200 bg-gray-50/50">
                          <div className="relative flex items-start gap-4">
                            <div className="w-12 h-12 rounded-xl bg-gray-200 animate-pulse"></div>
                            <div className="flex-1">
                              <div className="w-24 h-5 bg-gray-200 rounded animate-pulse mb-2"></div>
                              <div className="w-32 h-4 bg-gray-200 rounded animate-pulse"></div>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* Thông tin giao hàng Loading */}
                  <div className="mt-6 grid grid-cols-1 md:grid-cols-2 gap-6">
                    {[...Array(2)].map((_, colIndex) => (
                      <div key={colIndex} className="space-y-4">
                        {[...Array(2)].map((_, rowIndex) => (
                          <div key={rowIndex} className="flex items-center gap-2">
                            <div className="w-8 h-8 rounded-lg bg-gray-200 animate-pulse"></div>
                            <div>
                              <div className="w-24 h-4 bg-gray-200 rounded animate-pulse mb-1"></div>
                              <div className="w-32 h-5 bg-gray-200 rounded animate-pulse"></div>
                            </div>
                          </div>
                        ))}
                      </div>
                    ))}
                  </div>
                </div>

                {/* Footer Loading */}
                <div className="px-6 py-4 bg-gray-50 rounded-b-xl border-t flex items-center justify-between">
                  <div className="w-40 h-8 bg-gray-200 rounded-xl animate-pulse"></div>
                  <div className="flex gap-3">
                    <div className="w-24 h-9 bg-gray-200 rounded-lg animate-pulse"></div>
                    <div className="w-24 h-9 bg-gray-200 rounded-lg animate-pulse"></div>
                  </div>
                </div>
              </div>
            ))}

            {/* Pagination Loading */}
            <div className="flex justify-center items-center mt-8 gap-2">
              {[...Array(5)].map((_, index) => (
                <div key={index} className="w-10 h-10 bg-gray-200 rounded-lg animate-pulse"></div>
              ))}
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Thay thế phần return chính với:
  return (
    <div className={`min-h-screen ${
      theme === 'tet' 
        ? 'bg-gradient-to-br from-red-50 via-orange-50 to-yellow-50' 
        : 'bg-gradient-to-br from-blue-50 via-indigo-50 to-purple-50'
    }`}>
      <PageBanner
        theme={theme}
        icon={FaShoppingBag}
        title="Đơn Hàng Của Tôi"
        breadcrumbText="Đơn hàng"
        extraContent={
          <div className="flex items-center justify-center gap-3 text-xl text-white/90">
            <FaShoppingBag className="w-6 h-6" />
            <p>{orderStats.total} đơn hàng</p>
          </div>
        }
      />

      {/* Phần nội dung còn lại */}
      <div className="max-w-7xl mx-auto px-4 py-8 relative">
        {/* Phần thống kê đơn hàng - Thiết kế Glassmorphism */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          {/* Card tổng đơn hàng */}
          <div className="backdrop-blur-md bg-white/30 border border-white/50 rounded-2xl p-6 shadow-xl transform hover:scale-105 transition-all duration-300">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-600 font-medium">Tổng đơn hàng</p>
                <h3 className="text-3xl font-bold mt-2">{orderStats.total}</h3>
              </div>
              <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${
                theme === 'tet' ? 'bg-red-500/20' : 'bg-blue-500/20'
              }`}>
                <FaShoppingBag className={`text-xl ${
                  theme === 'tet' ? 'text-red-500' : 'text-blue-500'
                }`} />
              </div>
            </div>
          </div>

          {/* Card đơn hoàn thành */}
          <div className="backdrop-blur-md bg-white/30 border border-white/50 rounded-2xl p-6 shadow-xl transform hover:scale-105 transition-all duration-300">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-600 font-medium">Đã hoàn thành</p>
                <h3 className="text-3xl font-bold mt-2">{orderStats.completed}</h3>
              </div>
              <div className="w-12 h-12 rounded-xl bg-green-500/20 flex items-center justify-center">
                <FaCheck className="text-xl text-green-500" />
              </div>
            </div>
          </div>

          {/* Card tổng chi tiêu */}
          <div className="backdrop-blur-md bg-white/30 border border-white/50 rounded-2xl p-6 shadow-xl transform hover:scale-105 transition-all duration-300">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-600 font-medium">Tổng chi tiêu</p>
                <h3 className="text-3xl font-bold mt-2">{formatCurrency(orderStats.totalSpent)}đ</h3>
              </div>
              <div className={`w-12 h-12 rounded-xl ${
                theme === 'tet' ? 'bg-orange-500/20' : 'bg-purple-500/20'
              } flex items-center justify-center`}>
                <FaMoneyBillWave className={`text-xl ${
                  theme === 'tet' ? 'text-orange-500' : 'text-purple-500'
                }`} />
              </div>
            </div>
          </div>

          {/* Card tiết kiệm được */}
          <div className="backdrop-blur-md bg-white/30 border border-white/50 rounded-2xl p-6 shadow-xl transform hover:scale-105 transition-all duration-300">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-600 font-medium">Tiết kiệm được</p>
                <h3 className="text-3xl font-bold mt-2 text-green-500">{formatCurrency(orderStats.totalSaved)}đ</h3>
              </div>
              <div className="w-12 h-12 rounded-xl bg-green-500/20 flex items-center justify-center">
                <FaMoneyBillWave className="text-xl text-green-500" />
              </div>
            </div>
          </div>
        </div>

        {/* Phần header và công cụ */}
        <div className="backdrop-blur-md bg-white/50 rounded-2xl p-6 mb-8 border border-white/50 shadow-lg">
          <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
            <h1 className="text-2xl font-bold">Đơn hàng của tôi</h1>
            
            <div className="flex flex-col md:flex-row gap-4 w-full md:w-auto">
              {/* Ô tìm kiếm */}
              <div className="relative flex-1 md:flex-initial">
                <FaSearch className="absolute left-4 top-1/2 transform -translate-y-1/2 text-gray-400" />
                <input
                  type="text"
                  placeholder="Tìm kiếm đơn hàng..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full md:w-64 pl-12 pr-4 py-2.5 rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white/70"
                />
              </div>

              {/* Bộ lọc trạng thái và sắp xếp */}
              <div className="flex gap-3">
                <select
                  value={filterStatus}
                  onChange={(e) => setFilterStatus(e.target.value)}
                  className="px-4 py-2.5 rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white/70"
                >
                  <option value="all">Tất cả trạng thái</option>
                  <option value="pending">Chờ xác nhận</option>
                  <option value="confirmed">Đã xác nhận</option>
                  <option value="processing">Đang xử lý</option>
                  <option value="completed">Hoàn thành</option>
                  <option value="cancelled">Đã hủy</option>
                  <option value="refunded">Đã hoàn tiền</option>
                </select>

                <button
                  onClick={() => setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')}
                  className={`px-4 py-2.5 rounded-xl border border-gray-200 hover:bg-white/80 focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white/70 flex items-center gap-2 ${
                    theme === 'tet' ? 'hover:text-red-500' : 'hover:text-blue-500'
                  }`}
                >
                  <FaRegClock />
                  {sortOrder === 'asc' ? 'Cũ nhất' : 'Mới nhất'}
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Phần danh sách đơn hàng */}
        {orders.length === 0 ? (
          // Hiển thị khi không có đơn hàng
          <div className="text-center py-12 backdrop-blur-md bg-white/50 rounded-2xl border border-white/50 shadow-lg">
            <div className={`w-24 h-24 mx-auto mb-6 rounded-full flex items-center justify-center ${
              theme === 'tet' ? 'bg-red-100' : 'bg-blue-100'
            }`}>
              <FaBoxOpen className={`w-12 h-12 ${
                theme === 'tet' ? 'text-red-500' : 'text-blue-500'
              }`} />
            </div>
            <p className="text-gray-600 text-lg mb-6">Bạn chưa có đơn hàng nào</p>
            <Link
              to="/products"
              className={`inline-flex items-center px-6 py-3 rounded-xl text-white shadow-lg hover:shadow-xl transition-all duration-300 ${
                theme === 'tet'
                  ? 'bg-gradient-to-r from-red-500 to-orange-500 hover:from-red-600 hover:to-orange-600'
                  : 'bg-gradient-to-r from-blue-500 to-purple-500 hover:from-blue-600 hover:to-purple-600'
              }`}
            >
              <FaShoppingBag className="mr-2" />
              Mua sắm ngay
            </Link>
          </div>
        ) : (
          // Hiển thị danh sách đơn hàng
          <div className="grid gap-6">
            {currentOrders.map((order) => {
              const statusInfo = getStatusInfo(order.orderStatus);
              return (
                <div
                  key={order.orderID}
                  className="bg-white rounded-xl shadow-md hover:shadow-lg transition-all duration-300"
                >
                  {/* Header với thông tin cơ bản */}
                  <div className="p-6">
                    <div className="flex items-center justify-between mb-6">
                      <div className="flex items-center gap-4">
                        {/* Icon trạng thái */}
                        <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${statusInfo.bgColor}`}>
                          {statusInfo.icon}
                        </div>
                        
                        <div>
                          <h2 className="text-lg font-semibold">
                            Đơn hàng #{order.orderID}
                          </h2>
                          <div className="flex items-center gap-3 mt-1 text-sm text-gray-500">
                            <div className="flex items-center gap-1">
                              <FaRegClock className="w-3.5 h-3.5" />
                              <span>{new Date(order.createdAt).toLocaleDateString('vi-VN')}</span>
                            </div>
                            <span>•</span>
                            <span className={`${statusInfo.color}`}>
                              {statusInfo.text}
                            </span>
                          </div>
                        </div>
                      </div>

                      {/* Giá */}
                      <div className="text-right">
                        <div className="text-lg font-semibold">
                          {formatCurrency(order.paymentPrice)}đ
                        </div>
                        {order.totalPrice !== order.paymentPrice && (
                          <div className="text-sm">
                            <span className="text-gray-500 line-through">{formatCurrency(order.totalPrice)}đ</span>
                            <span className="ml-1 text-green-500">
                              (-{formatCurrency(order.totalPrice - order.paymentPrice)}đ)
                            </span>
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Timeline */}
                    <OrderTimeline order={order} />

                    {/* Thông tin giao hàng */}
                    <div className="mt-6 grid grid-cols-1 md:grid-cols-2 gap-6">
                      {/* Cột trái - Thông tin người nhận */}
                      <div className="space-y-4">
                        <div className="flex items-center gap-2 text-sm">
                          <div className="w-8 h-8 rounded-lg bg-blue-50 flex items-center justify-center">
                            <FaUser className="w-4 h-4 text-blue-500" />
                          </div>
                          <div>
                            <p className="text-gray-500">Người nhận</p>
                            <p className="font-medium">{order.fullname}</p>
                          </div>
                        </div>
                        <div className="flex items-center gap-2 text-sm">
                          <div className="w-8 h-8 rounded-lg bg-blue-50 flex items-center justify-center">
                            <FaMapMarkerAlt className="w-4 h-4 text-blue-500" />
                          </div>
                          <div>
                            <p className="text-gray-500">Địa chỉ giao hàng</p>
                            <p className="font-medium">{order.address}</p>
                          </div>
                        </div>
                      </div>

                      {/* Cột phải - Thông tin vận chuyển và thanh toán */}
                      <div className="space-y-4">
                        <div className="flex items-center gap-2 text-sm">
                          <div className="w-8 h-8 rounded-lg bg-blue-50 flex items-center justify-center">
                            <FaTruck className="w-4 h-4 text-blue-500" />
                          </div>
                          <div>
                            <p className="text-gray-500">Trạng thái vận chuyển</p>
                            <p className="font-medium">
                              {order.shippingStatus === 'preparing' && 'Đang chuẩn bị'}
                              {order.shippingStatus === 'shipping' && 'Đang giao hàng'}
                              {order.shippingStatus === 'delivered' && 'Đã giao hàng'}
                              {order.shippingStatus === 'returned' && 'Đã hoàn trả'}
                            </p>
                          </div>
                        </div>
                        <div className="flex items-center gap-2 text-sm">
                          <div className="w-8 h-8 rounded-lg bg-blue-50 flex items-center justify-center">
                            <FaMoneyBillWave className="w-4 h-4 text-blue-500" />
                          </div>
                          <div>
                            <p className="text-gray-500">Trạng thái thanh toán</p>
                            <p className="font-medium">
                              {order.isPayed ? 'Đã thanh toán' : 'Chưa thanh toán'}
                            </p>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Footer với các nút tác vụ */}
                  <div className="px-6 py-4 bg-gray-50 rounded-b-xl border-t flex items-center justify-between">
                    {/* Trạng thái chi tiết */}
                    {(order.orderStatus === 'processing' || order.shippingStatus === 'preparing') && (
                      <div className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-blue-50 text-blue-600 text-sm">
                        <div className="w-2 h-2 rounded-full bg-blue-500 animate-pulse" />
                        <span>
                          {order.orderStatus === 'processing' && 'Đang xử lý đơn hàng'} / {order.shippingStatus === 'preparing' && 'Đang chuẩn bị hàng'}
                        </span>
                      </div>
                    )}

                    {/* Nút tác vụ */}
                    <div className="flex gap-3">
                      <Link
                        to={`/order/${order.orderID}`}
                        className="px-6 py-2 rounded-lg bg-blue-500 text-white hover:bg-blue-600 transition-colors"
                      >
                        Chi tiết
                      </Link>
                      {order.orderStatus === 'pending' && (
                        <button
                          onClick={() => handleCancelOrder(order.orderID)}
                          className="px-6 py-2 rounded-lg bg-red-500 text-white hover:bg-red-600 transition-colors"
                        >
                          Hủy đơn
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}

            {/* Thêm phân trang */}
            <Pagination />
          </div>
        )}
      </div>
    </div>
  );
};

export default Orders;
