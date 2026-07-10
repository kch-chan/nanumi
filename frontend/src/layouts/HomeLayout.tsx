import { Outlet } from "react-router-dom";

function HomeLayout() {
  return (
    <div className="min-h-screen bg-stone-50">
        <Outlet />
    </div>
  );
}

export default HomeLayout;
