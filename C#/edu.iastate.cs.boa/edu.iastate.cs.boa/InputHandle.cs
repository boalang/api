using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace edu.iastate.cs.boa
{
    public class InputHandle
    {
        private int id;
        private string name;

        public InputHandle(int ID, String Name)
        {
            id = ID;
            name = Name;
        }

        public int getId()
        {
            return id;
        }

        public string getName()
        {
            return name;
        }

        public override string ToString()
        {
            return id + ", " + name;
        }
    }
}
