import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (r) => r,
  (error) => {
    const message =
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      'Request failed';
    return Promise.reject(new Error(message));
  }
);

export type UserRole = 'STUDENT' | 'LECTURER';
export type SessionType = 'LECTURE' | 'PRACTICAL' | 'TUTORIAL';

export interface AuthResponse {
  token: string;
  role: UserRole;
  userId: number;
  displayName: string;
}

export interface MeResponse {
  userId: number;
  role: UserRole;
  displayName: string;
  emailOrCode: string;
  indexNumber: string | null;
}

export interface Course {
  id: number;
  courseCode: string;
  courseName: string;
  departmentCode: string;
  departmentName: string;
}

export interface Session {
  id: number;
  courseId: number;
  courseCode: string;
  courseName: string;
  status: 'ACTIVE' | 'CLOSED';
  sessionType: SessionType;
  createdAt: string;
  presentCount: number;
}

export interface QrResponse {
  token: string;
  qrImageBase64: string;
  expiresAt: string;
}

export interface AttendanceRow {
  studentId: number;
  name: string;
  indexNumber: string;
  present: boolean;
  attendanceTime: string | null;
}

export interface SessionAttendance {
  sessionId: number;
  courseCode: string;
  rows: AttendanceRow[];
  presentCount: number;
  enrolledCount: number;
}

export interface HistoryItem {
  sessionId: number;
  courseCode: string;
  courseName: string;
  attendanceTime: string;
  status: string;
}

export interface LecturerStats {
  enrolled: number;
  present: number;
  absent: number;
  ratePercent: number;
  sessionId: number | null;
  courseCode: string | null;
}

export interface StudentStats {
  totalSessions: number;
  attended: number;
  missed: number;
  ratePercent: number;
}

export interface CourseAnalytics {
  courseCode: string;
  courseName: string;
  sessionsHeld: number;
  totalPresent: number;
  totalEnrolledSlots: number;
  averageRatePercent: number;
}

export async function downloadAttendanceCsv(sessionId: number) {
  const response = await api.get(`/sessions/${sessionId}/attendance/export`, {
    responseType: 'blob',
  });
  const url = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', `attendance-session-${sessionId}.csv`);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
