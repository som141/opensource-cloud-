export type UserRole = 'USER' | 'ADMIN';

export type User = {
  id: number;
  email: string;
  name: string;
  profileImageUrl?: string;
  role: UserRole;
  providers: string[];
};
