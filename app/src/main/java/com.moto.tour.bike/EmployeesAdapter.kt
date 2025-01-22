package com.moto.tour.bike

// Ваш адаптер
import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class EmployeesAdapter(private val employeeClickListener: EmployeeClickListener) : RecyclerView.Adapter<EmployeesAdapter.EmployeeViewHolder>() {

    private var employees: List<Employee> = emptyList()

    interface EmployeeClickListener {
        fun onEmployeeClick(employee: Employee)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.employee_item, parent, false)
        return EmployeeViewHolder(view)
    }

    override fun getItemCount(): Int {
        return employees.size
    }

    override fun onBindViewHolder(holder: EmployeeViewHolder, position: Int) {
        val employee = employees[position]
        holder.bind(employee)
    }

    fun submitList(newList: List<Employee>) {
        employees = newList
        notifyDataSetChanged()
    }

    inner class EmployeeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
        private val distanceTextView: TextView = itemView.findViewById(R.id.distanceTextView)
        private val itemLayout: LinearLayout = itemView.findViewById(R.id.lay)

        @SuppressLint("SetTextI18n")
        fun bind(employee: Employee) {
            Log.d("EmployeeViewHolder", "Username: ${employee.username}, Distance: ${employee.distance}")

            usernameTextView.text = employee.username
            distanceTextView.text = "%.1f км".format(employee.distance)

            itemLayout.setOnClickListener {
                // Вызовите слушатель нажатия и передайте экземпляр Employee
                employeeClickListener.onEmployeeClick(employee)
            }
        }
    }

    private class EmployeeDiffCallback : DiffUtil.ItemCallback<Employee>() {
        override fun areItemsTheSame(oldItem: Employee, newItem: Employee): Boolean {
            return oldItem.username == newItem.username
        }

        override fun areContentsTheSame(oldItem: Employee, newItem: Employee): Boolean {
            return oldItem == newItem
        }
    }
}
