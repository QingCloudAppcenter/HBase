import json
from xml.dom import minidom

import shutil

import sys

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print "Exit because of wrong input."
        sys.exit(1)

    xml_file = sys.argv[1]
    xml_new = "%s.new" % xml_file
    xml_tmp = "%s.tmp" % xml_file
    xml_tmp_object = open(xml_tmp, 'r')
    xml_tmp_content = xml_tmp_object.read()
    xml_tmp_object.close()
    xml_tmp_json = json.loads(xml_tmp_content)

    try:
        xml_tree = minidom.parse(xml_file)
        collection = xml_tree.documentElement
        properties = collection.getElementsByTagName("property")

        # modify
        for sub_property in properties:
            sub_name = sub_property.getElementsByTagName('name')[0]
            sub_value = sub_property.getElementsByTagName('value')[0]
            name = sub_name.childNodes[0].data
            value = sub_value.childNodes[0].data
            if name in xml_tmp_json:
                if xml_tmp_json[name] != value:
                    sub_value.childNodes[0].data = xml_tmp_json[name]
                xml_tmp_json.pop(name)

        # add
        for name, value in xml_tmp_json.items():
            new_property = xml_tree.createElement("property")

            name_element = xml_tree.createElement("name")
            name_text = xml_tree.createTextNode(name)
            name_element.appendChild(name_text)

            value_element = xml_tree.createElement("value")
            value_text = xml_tree.createTextNode(value)
            value_element.appendChild(value_text)

            new_property.appendChild(name_element)
            new_property.appendChild(value_element)

            collection.appendChild(new_property)

        # format and output xml
        f = open(xml_new, 'w')
        xml_content = xml_tree.toprettyxml(indent="    ", encoding='utf-8')
        for line in xml_content.splitlines():
            if line[:-1].strip():
                f.write(line+'\n')
        f.close()

        shutil.move(xml_new, xml_file)
    except Exception, e:
        print "Error: Cannot parse xml: %s" % e.message


