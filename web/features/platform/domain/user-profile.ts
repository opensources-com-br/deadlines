export type UserProfile = {
  id: string;
  email: string;
  profile: {
    firstName: string;
    lastName: string;
  };
};

export type UpdateUserProfileInput = {
  firstName: string;
  lastName: string;
};
